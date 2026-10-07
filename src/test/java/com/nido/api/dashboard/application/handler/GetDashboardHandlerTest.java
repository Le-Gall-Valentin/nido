package com.nido.api.dashboard.application.handler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.nido.api.dashboard.domain.model.AgendaCard;
import com.nido.api.dashboard.domain.model.AttentionItem;
import com.nido.api.dashboard.domain.model.AttentionKind;
import com.nido.api.dashboard.domain.model.CardKind;
import com.nido.api.dashboard.domain.model.CardResult;
import com.nido.api.dashboard.domain.model.Dashboard;
import com.nido.api.dashboard.domain.model.DashboardContext;
import com.nido.api.dashboard.domain.model.SourceResult;
import com.nido.api.dashboard.domain.port.out.DashboardPreparation;
import com.nido.api.dashboard.domain.port.out.DashboardSource;
import com.nido.api.shared.security.StoredValueRejected;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
import com.nido.api.space.application.port.in.GetSpaceUseCase;
import com.nido.api.space.domain.model.SpaceDetailView;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class GetDashboardHandlerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 26);
    private static final AgendaCard EMPTY_AGENDA = new AgendaCard(List.of(), List.of(), List.of(), null);

    private final SpaceMembership member = membership(SpaceRole.MEMBER);
    private final GetSpaceTodayUseCase spaceToday = spaceId -> TODAY;

    @Test
    void preparesBeforeReadingAnySource() {
        List<String> calls = new ArrayList<>();
        var handler = handler(SpaceType.SHARED,
            List.of(caller -> calls.add("prepare")),
            List.of(source(CardKind.AGENDA, context -> {
                calls.add("read");
                return SourceResult.of(EMPTY_AGENDA);
            })));

        handler.get(member);

        assertThat(calls).containsExactly("prepare", "read");
    }

    @Test
    void aFailingPreparationDoesNotStopTheReads() {
        var handler = handler(SpaceType.SHARED,
            List.of(caller -> { throw new IllegalStateException("lock timeout"); }),
            List.of(source(CardKind.AGENDA, context -> SourceResult.of(EMPTY_AGENDA))));

        Dashboard dashboard = handler.get(member);

        assertThat(dashboard.cards().get(CardKind.AGENDA)).isInstanceOf(CardResult.Ok.class);
    }

    @Test
    void aFailingSourceMarksOnlyItsOwnCardUnavailable() {
        var handler = handler(SpaceType.SHARED, List.of(), List.of(
            source(CardKind.FINANCE, context -> { throw new IllegalStateException("decryption failed"); }),
            source(CardKind.AGENDA, context -> SourceResult.of(EMPTY_AGENDA))));

        Dashboard dashboard = handler.get(member);

        assertThat(dashboard.cards().get(CardKind.FINANCE)).isInstanceOf(CardResult.Unavailable.class);
        assertThat(dashboard.cards().get(CardKind.AGENDA)).isEqualTo(new CardResult.Ok(EMPTY_AGENDA));
    }

    @Test
    void aRefusedStoredValueIsLoggedAsAnErrorAndOnlyItsCardIsUnavailable() {
        // Shown as unavailable like any failure, but raised as loudly as a page would raise it: the data was tampered with.
        var handler = handler(SpaceType.SHARED, List.of(caller -> { throw refused("finance_budgets.monthly_limit_encrypted"); }), List.of(
            source(CardKind.FINANCE, context -> { throw refused("finance_transactions.amount_encrypted"); }),
            source(CardKind.AGENDA, context -> SourceResult.of(EMPTY_AGENDA))));
        Logger logger = (Logger) LoggerFactory.getLogger(GetDashboardHandler.class);
        ListAppender<ILoggingEvent> logged = new ListAppender<>();
        logged.start();
        logger.addAppender(logged);
        try {
            Dashboard dashboard = handler.get(member);

            assertThat(dashboard.cards().get(CardKind.FINANCE)).isInstanceOf(CardResult.Unavailable.class);
            assertThat(dashboard.cards().get(CardKind.AGENDA)).isEqualTo(new CardResult.Ok(EMPTY_AGENDA));
            assertThat(logged.list).hasSize(2).allSatisfy(line -> assertThat(line.getLevel()).isEqualTo(Level.ERROR));
            assertThat(logged.list).extracting(ILoggingEvent::getFormattedMessage)
                .anySatisfy(line -> assertThat(line).contains("finance_budgets.monthly_limit_encrypted"))
                .anySatisfy(line -> assertThat(line).contains("finance_transactions.amount_encrypted"));
        } finally {
            logger.detachAppender(logged);
        }
    }

    private static StoredValueRejected refused(String place) {
        return new StoredValueRejected("The value of " + place + " in row 1 belongs to another place") {};
    }

    @Test
    void aFailingSourceWithoutACardStillMarksTheDashboardIncomplete() {
        // The invitations have no card to say "unavailable": the dashboard itself must know its
        // "À traiter" list is short of them, or the page would claim all is clear.
        var handler = handler(SpaceType.SHARED, List.of(), List.of(
            source(CardKind.INVITATIONS, context -> { throw new IllegalStateException("down"); }),
            source(CardKind.AGENDA, context -> SourceResult.of(EMPTY_AGENDA))));

        Dashboard dashboard = handler.get(member);

        assertThat(dashboard.cards().get(CardKind.INVITATIONS)).isInstanceOf(CardResult.Unavailable.class);
        assertThat(dashboard.complete()).isFalse();
    }

    @Test
    void aDashboardWhoseSourcesAllAnsweredIsComplete() {
        var handler = handler(SpaceType.SHARED, List.of(caller -> { throw new IllegalStateException("lock timeout"); }), List.of(
            source(CardKind.INVITATIONS, context -> SourceResult.nothing()),
            source(CardKind.AGENDA, context -> SourceResult.of(EMPTY_AGENDA))));

        // A failed preparation is not a missing answer: the sources still read what exists.
        assertThat(handler.get(member).complete()).isTrue();
    }

    @Test
    void aSourceWithNothingToShowLeavesNoEntry() {
        var handler = handler(SpaceType.SHARED, List.of(), List.of(
            source(CardKind.SHOPPING, context -> SourceResult.nothing())));

        assertThat(handler.get(member).cards()).isEmpty();
    }

    @Test
    void sortsAttentionBySeverityThenKind() {
        AttentionItem invitation = new AttentionItem.Invitation(UUID.randomUUID(), "Coloc", "🏠", "#c17a5c",
            SpaceRole.MEMBER, null, Instant.parse("2026-10-01T00:00:00Z"));
        AttentionItem debt = new AttentionItem.Debt(UUID.randomUUID(), new BigDecimal("42.50"));
        AttentionItem overrun = new AttentionItem.BudgetOverrun(UUID.randomUUID(), "Restaurants",
            new BigDecimal("212"), new BigDecimal("180"));
        AttentionItem overdue = new AttentionItem.OverdueTasks(2, List.of("a", "b"));
        var handler = handler(SpaceType.SHARED, List.of(), List.of(
            source(CardKind.INVITATIONS, context -> SourceResult.attentionOnly(List.of(invitation))),
            source(CardKind.FINANCE, context -> SourceResult.attentionOnly(List.of(debt, overrun))),
            source(CardKind.TASKS, context -> SourceResult.attentionOnly(List.of(overdue)))));

        assertThat(handler.get(member).attention())
            .extracting(AttentionItem::kind)
            .containsExactly(AttentionKind.OVERDUE_TASKS, AttentionKind.BUDGET_OVERRUN,
                AttentionKind.DEBT, AttentionKind.INVITATION);
    }

    @Test
    void everySourceReadsTheSpaceTodayItsTypeAndTheCallersEmail() {
        AtomicReference<DashboardContext> seen = new AtomicReference<>();
        var handler = handler(SpaceType.PERSONAL, List.of(), List.of(source(CardKind.AGENDA, context -> {
            seen.set(context);
            return SourceResult.of(EMPTY_AGENDA);
        })));

        Dashboard dashboard = handler.get(member);

        assertThat(seen.get()).isEqualTo(new DashboardContext(member, TODAY, SpaceType.PERSONAL));
        assertThat(dashboard.date()).isEqualTo(TODAY);
        assertThat(dashboard.spaceType()).isEqualTo(SpaceType.PERSONAL);
    }

    @Test
    void onlyARoleThatCanWriteGetsWriteRights() {
        var handler = handler(SpaceType.SHARED, List.of(), List.of());

        assertThat(handler.get(member).canWrite()).isTrue();
        assertThat(handler.get(membership(SpaceRole.VIEWER)).canWrite()).isFalse();
    }

    private GetDashboardHandler handler(SpaceType type, List<DashboardPreparation> preparations,
                                        List<DashboardSource> sources) {
        GetSpaceUseCase getSpace = (spaceId, caller) -> new SpaceDetailView(spaceId, type, "Maison", null,
            "#c17a5c", "🏡", ZoneId.of("Europe/Paris"), caller.role(), 2);
        return new GetDashboardHandler(spaceToday, getSpace, preparations, sources);
    }

    private static DashboardSource source(CardKind kind, Function<DashboardContext, SourceResult> read) {
        return new DashboardSource() {
            @Override
            public CardKind kind() {
                return kind;
            }

            @Override
            public SourceResult read(DashboardContext context) {
                return read.apply(context);
            }
        };
    }

    private static SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), role, Instant.now());
    }
}
