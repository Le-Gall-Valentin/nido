package com.nido.api.dashboard.application.handler;

import com.nido.api.dashboard.application.port.in.GetDashboardUseCase;
import com.nido.api.dashboard.domain.model.AttentionItem;
import com.nido.api.dashboard.domain.model.CardKind;
import com.nido.api.dashboard.domain.model.CardResult;
import com.nido.api.dashboard.domain.model.Dashboard;
import com.nido.api.dashboard.domain.model.DashboardContext;
import com.nido.api.dashboard.domain.model.SourceResult;
import com.nido.api.dashboard.domain.port.out.DashboardPreparation;
import com.nido.api.dashboard.domain.port.out.DashboardSource;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.shared.security.StoredValueRejected;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
import com.nido.api.space.application.port.in.GetSpaceUseCase;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the dashboard in two phases: prepare, then read.
 *
 * <p><b>Deliberately not {@code @Transactional}.</b> Every use case reached from here opens its own
 * transaction, read-only or not, so a write can never run inside a read-only one — the failure of
 * commit {@code 83fa623}, where the calendar's read-only transaction refused the materialization of a
 * recurring task and the whole calendar answered 500. The preparation phase relies on it: it
 * materializes the recurring occurrences that have fallen due, in write transactions, so that every
 * source afterwards reads the same, real rows whatever its order.
 *
 * <p><b>A failing source does not fail the read</b> — the opposite of
 * {@code ListCalendarOccurrencesHandler}, on purpose. A calendar missing a source lies: it shows a free
 * day. A dashboard card marked unavailable does not lie, and the other cards stay useful. Separate
 * transactions are what make this safe: a failing source cannot mark a shared transaction
 * rollback-only. Do not "align" this handler with the calendar's.
 */
@ApplicationService
public class GetDashboardHandler implements GetDashboardUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetDashboardHandler.class);

    private static final Comparator<AttentionItem> ATTENTION_ORDER =
        Comparator.comparing(AttentionItem::severity).thenComparing(AttentionItem::kind);

    private final GetSpaceTodayUseCase spaceToday;
    private final GetSpaceUseCase getSpace;
    private final List<DashboardPreparation> preparations;
    private final List<DashboardSource> sources;

    public GetDashboardHandler(GetSpaceTodayUseCase spaceToday, GetSpaceUseCase getSpace,
                               List<DashboardPreparation> preparations, List<DashboardSource> sources) {
        this.spaceToday = spaceToday;
        this.getSpace = getSpace;
        this.preparations = preparations;
        this.sources = sources;
    }

    @Override
    public Dashboard get(SpaceMembership caller) {
        LocalDate today = spaceToday.today(caller.spaceId());
        SpaceType spaceType = getSpace.get(caller.spaceId(), caller).type();

        for (DashboardPreparation preparation : preparations) {
            try {
                preparation.prepare(caller);
            } catch (RuntimeException e) {
                if (e instanceof StoredValueRejected refused) {
                    log.error("Dashboard preparation {} refused a stored value in space {}; reading what exists: {}",
                        preparation.getClass().getSimpleName(), caller.spaceId(), refused.getMessage());
                } else {
                    log.warn("Dashboard preparation {} failed in space {}; reading what exists",
                        preparation.getClass().getSimpleName(), caller.spaceId(), e);
                }
            }
        }

        DashboardContext context = new DashboardContext(caller, today, spaceType);
        Map<CardKind, CardResult> cards = new EnumMap<>(CardKind.class);
        List<AttentionItem> attention = new ArrayList<>();
        for (DashboardSource source : sources) {
            SourceResult result;
            try {
                result = source.read(context);
            } catch (RuntimeException e) {
                if (e instanceof StoredValueRejected refused) {
                    // Unavailable like any failure, but as loud as a page would be: the data was tampered with.
                    log.error("Dashboard source {} refused a stored value in space {}: {}", source.kind(), caller.spaceId(),
                        refused.getMessage());
                } else {
                    log.warn("Dashboard source {} failed in space {}", source.kind(), caller.spaceId(), e);
                }
                // Recorded even for a kind without a card: the response hides it from the cards but
                // reports the dashboard incomplete (Dashboard#complete).
                cards.put(source.kind(), new CardResult.Unavailable());
                continue;
            }
            if (result.card() != null) {
                cards.put(source.kind(), new CardResult.Ok(result.card()));
            }
            attention.addAll(result.attention());
        }
        attention.sort(ATTENTION_ORDER);

        return new Dashboard(today, spaceType, caller.role().canWrite(), attention, cards);
    }
}
