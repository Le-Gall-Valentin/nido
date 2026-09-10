package com.nido.api.tasks.domain.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * Computes a recurring series' due dates as a fixed calendar sequence
 * anchored to its start date — never relative to when each occurrence
 * actually gets completed, so a late or early completion never shifts the
 * schedule. Occurrence 0 is the anchor date itself; occurrence N is N
 * intervals after it. Monthly anchoring on a day a target month doesn't
 * have (e.g. the 31st) clamps to that month's last day, but every later
 * occurrence is still computed from the original anchor day, not from the
 * clamped value — so a Jan-31 anchor lands on Feb 28 and then back on
 * Mar 31, never drifting to Mar 28.
 */
public final class RecurrenceScheduler {

    /**
     * Largest pending backlog a series may carry when it is created or updated.
     * Mirrors Finance's own ceiling: a year of daily occurrences, comfortably above
     * any legitimate back-dating, far below what would strain a single request.
     */
    public static final int MAX_BACKLOG_OCCURRENCES = 365;

    private RecurrenceScheduler() {}

    public static LocalDate nextDueDate(LocalDate anchorDate, RecurrenceInterval intervalType, int intervalCount, int occurrenceNumber) {
        long steps = (long) intervalCount * occurrenceNumber;
        return switch (intervalType) {
            case DAILY -> anchorDate.plusDays(steps);
            case WEEKLY -> anchorDate.plusWeeks(steps);
            case MONTHLY -> {
                YearMonth targetMonth = YearMonth.from(anchorDate).plusMonths(steps);
                int day = Math.min(anchorDate.getDayOfMonth(), targetMonth.lengthOfMonth());
                yield targetMonth.atDay(day);
            }
            case YEARLY -> anchorDate.plusYears(steps);
        };
    }

    /**
     * Shifts a concrete date backward by a single interval — used to find where a
     * materialized occurrence's lead-time visibility window opens. Unlike
     * {@link #nextDueDate}, this is a one-off shift of an arbitrary date, not a
     * repeated re-derivation from a fixed anchor, so the JDK's own end-of-month
     * clamping (e.g. {@code LocalDate.of(2026,3,31).minusMonths(1)} → 2026-02-28)
     * is already correct with no extra logic needed.
     */
    public static LocalDate minus(LocalDate date, RecurrenceInterval intervalType, int intervalCount) {
        return switch (intervalType) {
            case DAILY -> date.minusDays(intervalCount);
            case WEEKLY -> date.minusWeeks(intervalCount);
            case MONTHLY -> date.minusMonths(intervalCount);
            case YEARLY -> date.minusYears(intervalCount);
        };
    }

    /**
     * Validates the two schedule invariants shared by every recurring series' create/update
     * command: the lead time never opens an occurrence's window before the previous one's due
     * date, and an end date is never before the anchor date (which would never produce a single
     * occurrence).
     */
    public static void validateSchedule(LocalDate anchorDate, RecurrenceInterval intervalType, int intervalCount,
                                         RecurrenceInterval leadIntervalType, int leadIntervalCount, LocalDate endDate) {
        LocalDate leadDate = nextDueDate(anchorDate, leadIntervalType, leadIntervalCount, 1);
        LocalDate mainDate = nextDueDate(anchorDate, intervalType, intervalCount, 1);
        if (leadDate.isAfter(mainDate)) {
            throw new TaskException.LeadTimeExceedsInterval();
        }
        if (endDate != null && endDate.isBefore(anchorDate)) {
            throw new TaskException.InvalidEndDate();
        }
    }

    /**
     * Index of the first occurrence falling on or after {@code target}, computed
     * arithmetically instead of by stepping through every occurrence since the anchor.
     *
     * <p>The division gives an estimate that can be off by one — month-end clamping means
     * an occurrence's calendar month, not its exact date, is what the estimate captures —
     * so two bounded correction steps settle it. Returns {@link Integer#MAX_VALUE} when the
     * estimate overflows an {@code int}, which callers read as "further away than anything
     * worth producing".
     */
    static long firstOccurrenceIndexOnOrAfter(LocalDate anchorDate, RecurrenceInterval intervalType,
                                              int intervalCount, LocalDate target) {
        if (intervalCount < 1) {
            throw new IllegalArgumentException("intervalCount must be at least 1, got " + intervalCount);
        }
        if (!target.isAfter(anchorDate)) {
            return 0L;
        }
        long estimate = switch (intervalType) {
            case DAILY -> ChronoUnit.DAYS.between(anchorDate, target) / intervalCount;
            case WEEKLY -> ChronoUnit.WEEKS.between(anchorDate, target) / intervalCount;
            case MONTHLY -> ChronoUnit.MONTHS.between(YearMonth.from(anchorDate), YearMonth.from(target)) / intervalCount;
            case YEARLY -> ChronoUnit.YEARS.between(anchorDate, target) / intervalCount;
        };
        if (estimate >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        int n = (int) Math.max(0L, estimate);
        while (n > 0 && !nextDueDate(anchorDate, intervalType, intervalCount, n - 1).isBefore(target)) {
            n--;
        }
        while (nextDueDate(anchorDate, intervalType, intervalCount, n).isBefore(target)) {
            n++;
        }
        return n;
    }

    /**
     * How many occurrences a series still owes: those due on or before {@code today}
     * (or {@code endDate}, whichever comes first) that it has not generated yet.
     *
     * <p>Counted by due date, while the materializer opens an occurrence's window a lead
     * time earlier. Since {@link #validateSchedule} guarantees the lead time never exceeds
     * one interval, that can only ever bring a single further occurrence into range — a
     * rounding of one against a ceiling in the hundreds, deliberately ignored rather than
     * duplicating the lead-time arithmetic here.
     */
    static long pendingOccurrenceCount(LocalDate anchorDate, RecurrenceInterval intervalType, int intervalCount,
                                       LocalDate endDate, LocalDate today, int alreadyGeneratedCount) {
        LocalDate horizon = endDate != null && endDate.isBefore(today) ? endDate : today;
        if (horizon.isBefore(anchorDate)) {
            return 0L;
        }
        long lastDueIndex = horizon.equals(LocalDate.MAX)
            ? Integer.MAX_VALUE
            : firstOccurrenceIndexOnOrAfter(anchorDate, intervalType, intervalCount, horizon.plusDays(1)) - 1;
        return Math.max(0L, lastDueIndex - alreadyGeneratedCount);
    }

    /**
     * Refuses a series whose pending backlog exceeds {@link #MAX_BACKLOG_OCCURRENCES}.
     *
     * <p>What counts is what is still <em>owed</em>, not how old the anchor is: a long-running
     * series that has already generated its occurrences carries no backlog, so editing its
     * title stays possible. {@code alreadyGeneratedCount} is 0 for a series being created.
     */
    public static void validateBacklog(LocalDate anchorDate, RecurrenceInterval intervalType, int intervalCount,
                                       LocalDate endDate, LocalDate today, int alreadyGeneratedCount) {
        long backlog = pendingOccurrenceCount(anchorDate, intervalType, intervalCount, endDate, today, alreadyGeneratedCount);
        if (backlog > MAX_BACKLOG_OCCURRENCES) {
            throw new TaskException.RecurrenceBacklogTooLarge(backlog, MAX_BACKLOG_OCCURRENCES);
        }
    }
}
