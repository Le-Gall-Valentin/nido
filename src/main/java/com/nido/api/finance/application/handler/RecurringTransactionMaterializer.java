package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.CreateTransactionCommand;
import com.nido.api.finance.domain.model.RecurrenceProjector;
import com.nido.api.finance.domain.model.RecurringSeriesSchedule;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.port.out.RecurringTransactionSeriesRepository;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 *
 * <p>Each pass writes at most {@link #MAX_OCCURRENCES_PER_RUN} occurrences per series.
 * Without that ceiling the amount of work a single read performs is dictated by how far
 * back a series was anchored, which is caller-supplied — a series anchored decades ago
 * would generate hundreds of thousands of rows inside one transaction, holding the
 * space's advisory lock throughout. {@code RecurrenceProjector.validateBacklog} already
 * refuses such a series at creation; this ceiling is what protects series that predate
 * that rule, or that fell far behind while nobody opened the space.
 */
final class RecurringTransactionMaterializer {

    private static final Logger log = LoggerFactory.getLogger(RecurringTransactionMaterializer.class);

    /**
     * Occurrences a single pass may materialize per series. Generous enough that any
     * realistic catch-up — well over a year of daily occurrences — completes in one pass,
     * low enough that a pathological series costs a bounded number of inserts per request.
     */
    static final int MAX_OCCURRENCES_PER_RUN = 500;

    private RecurringTransactionMaterializer() {}

    static void materializeDueOccurrences(
            TransactionRepository transactionRepository, RecurringTransactionSeriesRepository seriesRepository,
            UUID spaceId, LocalDate today) {
        // Nothing due is the overwhelmingly common case: a series comes due once a month,
        // and its space gets read many times a day. Deciding that from the schedules alone —
        // no lock, no entity hydration, no decryption — is what keeps an ordinary finance read
        // from opening a write-intent transaction and serializing every other reader of the
        // same space behind an advisory lock it had no work to justify.
        if (seriesRepository.findSchedulesBySpaceId(spaceId).stream()
                .noneMatch(schedule -> hasDueOccurrence(schedule, today))) {
            return;
        }

        // Several read paths (list, stats, projection) can race to materialize the same
        // space on a single page load — this blocks concurrent callers until whichever one
        // got here first has committed its advanced lastMaterializedDate, so they see it's
        // already covered instead of double-inserting the same occurrence.
        //
        // The check above is deliberately not trusted past this point: it ran unlocked, so by
        // now another request may have materialized everything it saw as due. Everything below
        // re-reads under the lock, and finding nothing left to do is a normal outcome.
        seriesRepository.lockForMaterialization(spaceId);
        // Deliberately not findActiveBySpaceId(spaceId, today): a series whose endDate has
        // already passed by the time anyone opens the page would be excluded from that query
        // entirely, silently dropping any of its occurrences that were never materialized
        // while it was still "active" (e.g. a fixed-term loan nobody checked on until after
        // it ended). occurrencesBetween already stops at series.endDate() on its own, so
        // considering every series here is safe and still correct.
        for (RecurringTransactionSeries series : seriesRepository.findBySpaceId(spaceId)) {
            LocalDate from = firstUnmaterializedDate(series.anchorDate(), series.lastMaterializedDate());
            if (from.isAfter(today)) {
                continue;
            }
            List<LocalDate> due = RecurrenceProjector.occurrencesBetween(
                series.anchorDate(), series.intervalType(), series.intervalCount(), series.endDate(), from, today,
                MAX_OCCURRENCES_PER_RUN);
            if (due.isEmpty()) {
                continue;
            }
            // series.contributors() is already a resolved List<Contribution> (fixed at series
            // creation time by CreateRecurringSeriesHandler) — every materialized occurrence
            // reuses those same fixed shares verbatim, no re-splitting, no re-validation.
            for (LocalDate date : due) {
                transactionRepository.create(new CreateTransactionCommand(spaceId, series.label(), series.amount(),
                    series.type(), series.categoryId(), date, series.payerId(), List.of(), series.id()), series.contributors());
            }
            // Advancing the cursor once per series rather than once per occurrence is just as
            // safe: this whole method already runs inside the caller's single @Transactional
            // boundary, so a crash partway through rolls back every create() above alongside
            // this update regardless — but it cuts what used to be 2 reads + 1 write per
            // overdue occurrence (hundreds of round trips for a long-neglected daily series)
            // down to a single write per series.
            LocalDate lastMaterialized = due.get(due.size() - 1);
            seriesRepository.advanceLastMaterializedDate(series.id(), lastMaterialized);
            if (due.size() == MAX_OCCURRENCES_PER_RUN) {
                // Not an error: the cursor stopped on the last occurrence written, so the next
                // read resumes from there. Worth a warning all the same — a series that keeps
                // hitting the ceiling was anchored further back than anyone intended.
                log.warn("Recurring series {} hit the {}-occurrence materialization ceiling in space {}; "
                        + "caught up to {}, remainder deferred to the next read",
                    series.id(), MAX_OCCURRENCES_PER_RUN, spaceId, lastMaterialized);
            }
        }
    }
    /**
     * Whether this series owes at least one occurrence on or before {@code today}.
     *
     * <p>Must never answer "no" where the loop below would have found something: an occurrence
     * missed here is not materialized at all until the next read that happens to see it. Both
     * derive the window from {@link #firstUnmaterializedDate}, and
     * {@code RecurrenceProjector.occurrenceCountBetween} is pinned to the dates
     * {@code occurrencesBetween} actually produces by its own test — so the two cannot drift
     * apart without that test failing.
     */
    private static boolean hasDueOccurrence(RecurringSeriesSchedule schedule, LocalDate today) {
        LocalDate from = firstUnmaterializedDate(schedule.anchorDate(), schedule.lastMaterializedDate());
        if (from.isAfter(today)) {
            return false;
        }
        return RecurrenceProjector.occurrenceCountBetween(
            schedule.anchorDate(), schedule.intervalType(), schedule.intervalCount(), schedule.endDate(),
            from, today) > 0;
    }

    /** The earliest date this series has not yet accounted for. A null cursor means none of it. */
    private static LocalDate firstUnmaterializedDate(LocalDate anchorDate, LocalDate lastMaterializedDate) {
        return lastMaterializedDate == null ? anchorDate : lastMaterializedDate.plusDays(1);
    }
}
