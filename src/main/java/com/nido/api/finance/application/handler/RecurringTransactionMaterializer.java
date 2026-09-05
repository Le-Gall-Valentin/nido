package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.CreateTransactionCommand;
import com.nido.api.finance.domain.model.RecurrenceProjector;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.port.out.RecurringTransactionSeriesRepository;
import com.nido.api.finance.domain.port.out.TransactionRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Lazily materializes any due-but-not-yet-materialized recurring
 * transaction occurrence into a real, persisted {@code Transaction} —
 * shared by every read path that must reflect "what's happened so far"
 * (listing transactions, computing stats), so a recurring series never
 * silently misses an occurrence regardless of which endpoint is hit first.
 * Never materializes anything after {@code today} — see
 * {@code GetProjectionHandler} (Task 14) for future occurrences, which are
 * only ever computed in memory.
 */
final class RecurringTransactionMaterializer {

    private RecurringTransactionMaterializer() {}

    static void materializeDueOccurrences(
            TransactionRepository transactionRepository, RecurringTransactionSeriesRepository seriesRepository,
            UUID spaceId, LocalDate today) {
        // Several read paths (list, stats, projection) can race to materialize the same
        // space on a single page load — this blocks concurrent callers until whichever one
        // got here first has committed its advanced lastMaterializedDate, so they see it's
        // already covered instead of double-inserting the same occurrence.
        seriesRepository.lockForMaterialization(spaceId);
        // Deliberately not findActiveBySpaceId(spaceId, today): a series whose endDate has
        // already passed by the time anyone opens the page would be excluded from that query
        // entirely, silently dropping any of its occurrences that were never materialized
        // while it was still "active" (e.g. a fixed-term loan nobody checked on until after
        // it ended). occurrencesBetween already stops at series.endDate() on its own, so
        // considering every series here is safe and still correct.
        for (RecurringTransactionSeries series : seriesRepository.findBySpaceId(spaceId)) {
            LocalDate from = series.lastMaterializedDate() == null ? series.anchorDate() : series.lastMaterializedDate().plusDays(1);
            if (from.isAfter(today)) {
                continue;
            }
            List<LocalDate> due = RecurrenceProjector.occurrencesBetween(
                series.anchorDate(), series.intervalType(), series.intervalCount(), series.endDate(), from, today);
            // series.contributors() is already a resolved List<Contribution> (fixed at series
            // creation time by CreateRecurringSeriesHandler) — every materialized occurrence
            // reuses those same fixed shares verbatim, no re-splitting, no re-validation.
            for (LocalDate date : due) {
                transactionRepository.create(new CreateTransactionCommand(spaceId, series.label(), series.amount(),
                    series.type(), series.categoryId(), date, series.payerId(), List.of(), series.id()), series.contributors());
                seriesRepository.advanceLastMaterializedDate(series.id(), date);
            }
        }
    }
}
