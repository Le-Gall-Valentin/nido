package com.nido.api.finance.domain.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes a recurring series' occurrence dates as a fixed calendar
 * sequence anchored to its start date — the same anchored-date math as
 * Tasks' {@code RecurrenceScheduler}, but exposed as a range query rather
 * than a single next-date lookup: Finance never pre-generates future rows,
 * so both lazy materialization (occurrences up to today) and month-ahead
 * projection (occurrences in a future month, never persisted) go through
 * {@link #occurrencesBetween}.
 *
 * <p>Two invariants keep the lazy-materialization path bounded, whatever a
 * caller submits. {@link #occurrencesBetween} takes an explicit {@code limit}
 * — no caller can ask for an unbounded range — and it locates its starting
 * occurrence arithmetically rather than by stepping from occurrence 0, so its
 * cost depends on how many dates it returns, never on how old the anchor is.
 * {@link #validateBacklog} then refuses, up front, a series whose pending
 * backlog would be absurd, so the limit is a safety net rather than the
 * everyday mechanism.
 */
public final class RecurrenceProjector {

    /**
     * Largest pending backlog a series may carry when it is created or updated.
     * A year of daily occurrences, forty years of monthly ones — comfortably above
     * any legitimate back-dating, far below what would strain a single request.
     */
    public static final int MAX_BACKLOG_OCCURRENCES = 365;

    private RecurrenceProjector() {}

    public static LocalDate occurrenceDate(LocalDate anchorDate, RecurrenceInterval intervalType, int intervalCount, int occurrenceNumber) {
        // Not reachable through the real API today (RecurrenceRequest.intervalCount has
        // @Min(1)), but occurrencesBetween's scan below never advances and loops forever
        // if this is allowed through some other path — fail fast instead.
        if (intervalCount < 1) {
            throw new IllegalArgumentException("intervalCount must be at least 1, got " + intervalCount);
        }
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
     * Index of the first occurrence falling on or after {@code target}, computed
     * arithmetically instead of by stepping through every occurrence since the anchor.
     *
     * <p>The division gives an estimate that can be off by one — month-end clamping means
     * an occurrence's calendar month, not its exact date, is what the estimate captures —
     * so two bounded correction steps settle it. Both loops run at most a couple of times,
     * which is what keeps this O(1): a series anchored decades ago costs the same as one
     * anchored yesterday.
     *
     * <p>Returns {@link Integer#MAX_VALUE} when the estimate overflows an {@code int}
     * rather than attempting a correction it could not compute. Callers treat that as
     * "further away than anything worth producing": {@link #occurrencesBetween} yields
     * nothing and {@link #validateBacklog} rejects, both of which are the right answer.
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
        while (n > 0 && !occurrenceDate(anchorDate, intervalType, intervalCount, n - 1).isBefore(target)) {
            n--;
        }
        while (occurrenceDate(anchorDate, intervalType, intervalCount, n).isBefore(target)) {
            n++;
        }
        return n;
    }

    /**
     * Every occurrence date in {@code [from, to]}, ascending, stopping at {@code endDate}
     * when one is set and at {@code limit} dates in any case.
     *
     * <p>A truncated result is not an error: the materialization cursor advances to the last
     * date returned, so the next pass resumes exactly where this one stopped. Spreading a
     * long catch-up over several passes keeps any single request bounded while still
     * eventually producing every occurrence.
     */
    public static List<LocalDate> occurrencesBetween(
            LocalDate anchorDate, RecurrenceInterval intervalType, int intervalCount, LocalDate endDate,
            LocalDate from, LocalDate to, int limit) {
        if (limit <= 0 || to.isBefore(from)) {
            return List.of();
        }
        long startIndex = firstOccurrenceIndexOnOrAfter(anchorDate, intervalType, intervalCount, from);
        if (startIndex >= Integer.MAX_VALUE) {
            return List.of();
        }
        List<LocalDate> result = new ArrayList<>();
        int n = (int) startIndex;
        LocalDate date = occurrenceDate(anchorDate, intervalType, intervalCount, n);
        while (!date.isAfter(to) && (endDate == null || !date.isAfter(endDate)) && result.size() < limit) {
            result.add(date);
            n++;
            date = occurrenceDate(anchorDate, intervalType, intervalCount, n);
        }
        return result;
    }

    /** How many occurrences fall in {@code [from, to]}, without building a single date. */
    public static long occurrenceCountBetween(
            LocalDate anchorDate, RecurrenceInterval intervalType, int intervalCount, LocalDate endDate,
            LocalDate from, LocalDate to) {
        LocalDate horizon = endDate != null && endDate.isBefore(to) ? endDate : to;
        if (horizon.isBefore(from)) {
            return 0L;
        }
        long firstIndex = firstOccurrenceIndexOnOrAfter(anchorDate, intervalType, intervalCount, from);
        long afterIndex = horizon.equals(LocalDate.MAX)
            ? Integer.MAX_VALUE
            : firstOccurrenceIndexOnOrAfter(anchorDate, intervalType, intervalCount, horizon.plusDays(1));
        return Math.max(0L, afterIndex - firstIndex);
    }

    /**
     * Refuses a series whose pending backlog exceeds {@link #MAX_BACKLOG_OCCURRENCES}.
     *
     * <p>What counts is what is still <em>owed</em>, not how old the anchor is: an existing
     * series already materialized up to last week carries no backlog however far back it was
     * anchored, so editing its label stays possible. Passing {@code null} for
     * {@code lastMaterializedDate} measures from the anchor itself, which is the case of a
     * series being created.
     */
    public static void validateBacklog(LocalDate anchorDate, RecurrenceInterval intervalType, int intervalCount,
                                       LocalDate endDate, LocalDate today, LocalDate lastMaterializedDate) {
        LocalDate from = lastMaterializedDate == null ? anchorDate : lastMaterializedDate.plusDays(1);
        long backlog = occurrenceCountBetween(anchorDate, intervalType, intervalCount, endDate, from, today);
        if (backlog > MAX_BACKLOG_OCCURRENCES) {
            throw new FinanceException.RecurrenceBacklogTooLarge(backlog, MAX_BACKLOG_OCCURRENCES);
        }
    }
}
