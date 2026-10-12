package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.GetProjectionUseCase;
import com.nido.api.finance.application.service.RunningBalanceReader;
import com.nido.api.finance.domain.model.ProjectedOccurrence;
import com.nido.api.finance.domain.model.Projection;
import com.nido.api.finance.domain.model.RecurrenceProjector;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.port.out.RecurringTransactionSeriesRepository;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@ApplicationService
public class GetProjectionHandler implements GetProjectionUseCase {

    /**
     * Ceiling on the occurrences a single series may contribute to one month's projection.
     * A calendar month holds at most 31 daily occurrences, so this never binds in practice —
     * it is here so no call site of occurrencesBetween can ask for an unbounded range.
     */
    private static final int MAX_PROJECTED_OCCURRENCES_PER_SERIES = 100;

    private final GetSpaceTodayUseCase spaceToday;
    private final TransactionRepository transactionRepository;
    private final RecurringTransactionSeriesRepository seriesRepository;
    private final RunningBalanceReader runningBalanceReader;

    public GetProjectionHandler(TransactionRepository transactionRepository, RecurringTransactionSeriesRepository seriesRepository,
                            RunningBalanceReader runningBalanceReader, GetSpaceTodayUseCase spaceToday) {
        this.transactionRepository = transactionRepository;
        this.seriesRepository = seriesRepository;
        this.runningBalanceReader = runningBalanceReader;
        this.spaceToday = spaceToday;
    }

    @Override
    @Transactional
    public Projection getProjection(YearMonth month, SpaceMembership caller) {
        return getProjection(month, caller, spaceToday.today(caller.spaceId()));
    }

    /**
     * Package-visible overload with an explicit "today" — lets tests project deterministically.
     * Annotated in its own right (not just via the public overload) so an external caller invoking
     * it directly through the Spring proxy — e.g. an integration test — still gets the transaction
     * boundary the advisory lock in {@code RecurringTransactionMaterializer} depends on.
     */
    @Transactional
    Projection getProjection(YearMonth month, SpaceMembership caller, LocalDate today) {
        RecurringTransactionMaterializer.materializeDueOccurrences(transactionRepository, seriesRepository, caller.spaceId(), today);
        // Everything recorded up to the month end, carried over from the months before: an operation entered for later
        // counts in it, a recurring one only once created.
        BigDecimal recorded = runningBalanceReader.read(caller.spaceId()).atEndOf(month);

        // Every recurring operation still to come before the month ends is added — those of the months between now and
        // then too, or a month ahead would be estimated as though no rent fell due before it. From tomorrow, whatever
        // the month: today's occurrences were just created and are recorded, on the 1st too. They are counted, not
        // listed one by one, so a month years ahead costs no more than the next one; only the month's own are listed.
        LocalDate afterToday = today.plusDays(1);
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();
        LocalDate listedFrom = afterToday.isAfter(monthStart) ? afterToday : monthStart;

        List<ProjectedOccurrence> upcoming = new ArrayList<>();
        BigDecimal projectedDelta = BigDecimal.ZERO;
        if (!afterToday.isAfter(monthEnd)) {
            for (RecurringTransactionSeries series : seriesRepository.findActiveBySpaceId(caller.spaceId(), afterToday)) {
                long dueBeforeTheMonthEnds = RecurrenceProjector.occurrenceCountBetween(
                    series.anchorDate(), series.intervalType(), series.intervalCount(), series.endDate(), afterToday, monthEnd);
                projectedDelta = projectedDelta.add(series.type().signed(series.amount()).multiply(BigDecimal.valueOf(dueBeforeTheMonthEnds)));
                List<LocalDate> dates = RecurrenceProjector.occurrencesBetween(
                    series.anchorDate(), series.intervalType(), series.intervalCount(), series.endDate(), listedFrom, monthEnd,
                    MAX_PROJECTED_OCCURRENCES_PER_SERIES);
                for (LocalDate date : dates) {
                    upcoming.add(new ProjectedOccurrence(series.id(), series.label(), series.amount(), series.type(), date));
                }
            }
        }

        return new Projection(recorded, upcoming, recorded.add(projectedDelta));
    }
}
