package com.nido.api.calendar.domain.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * Validates a command's schedule <em>before</em> it reaches the database.
 *
 * <p>The same rules are carried by the records and by the table's CHECK constraints, but neither
 * is reachable in time: the record is only built from the row after the insert, and letting the
 * constraint fire turns a user mistake into a DataIntegrityViolationException, which surfaces as
 * a 500 rather than a 400 naming what is wrong.
 */
public final class EventScheduleValidator {

    /** The most days an event may cover, or an occurrence of a series: no more than a single calendar read can show. */
    public static final int MAX_EVENT_DAYS = EventRecurrenceProjector.MAX_WINDOW_DAYS;

    /** nido cannot sensibly say a series repeating more rarely than this, and date arithmetic on it would overflow. */
    public static final int MAX_INTERVAL = 1000;

    private static final long MINUTES_PER_DAY = 1440;

    private EventScheduleValidator() {}

    public static void validateEvent(boolean allDay, LocalDate startDate, LocalTime startTime,
                                     LocalDate endDate, LocalTime endTime) {
        if (startDate == null || endDate == null) {
            throw new CalendarException.InvalidTimeRange();
        }
        if (allDay != (startTime == null) || (startTime == null) != (endTime == null)) {
            throw new CalendarException.InvalidTimeRange();
        }
        if (endDate.isBefore(startDate)) {
            throw new CalendarException.InvalidTimeRange();
        }
        // Only a single-day timed event can have its times compared: across days, an end time
        // earlier than the start time is perfectly ordinary (an overnight event).
        if (!allDay && startDate.equals(endDate) && endTime.isBefore(startTime)) {
            throw new CalendarException.InvalidTimeRange();
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) + 1 > MAX_EVENT_DAYS) {
            throw new CalendarException.EventTooLong(MAX_EVENT_DAYS);
        }
    }

    public static void validateSeries(boolean allDay, LocalTime startTime, LocalTime endTime, int durationDays,
                                      RecurrenceInterval intervalType, int intervalCount,
                                      LocalDate anchorDate, LocalDate endDate) {
        validateSeries(allDay, startTime, endTime, durationDays, intervalType, intervalCount, null, null, anchorDate, endDate);
    }

    public static void validateSeries(boolean allDay, LocalTime startTime, LocalTime endTime, int durationDays,
                                      RecurrenceInterval intervalType, int intervalCount,
                                      Set<DayOfWeek> weekdays, DayRule dayRule,
                                      LocalDate anchorDate, LocalDate endDate) {
        if (anchorDate == null || durationDays < 0) {
            throw new CalendarException.InvalidTimeRange();
        }
        if (allDay != (startTime == null) || (startTime == null) != (endTime == null)) {
            throw new CalendarException.InvalidTimeRange();
        }
        if (!allDay && durationDays == 0 && endTime.isBefore(startTime)) {
            throw new CalendarException.InvalidTimeRange();
        }
        if (endDate != null && endDate.isBefore(anchorDate)) {
            throw new CalendarException.InvalidEndDate();
        }
        if (intervalCount > MAX_INTERVAL) {
            throw new CalendarException.IntervalTooLarge(MAX_INTERVAL);
        }
        validateRule(intervalType, weekdays, dayRule, anchorDate);
        // An occurrence never runs into the next one, as in any diary. It is also what bounds a read:
        // every occurrence overlapping a window then starts at most one interval before it.
        long shortest = shortestIntervalDays(intervalType, intervalCount,
            intervalType == RecurrenceInterval.WEEKLY ? Weekdays.forWeekly(weekdays, anchorDate) : weekdays,
            dayRule == null ? DayRule.SAME_DAY : dayRule);
        if (allDay
            ? durationDays + 1L > shortest
            : durationDays * MINUTES_PER_DAY + minutesOf(endTime) - minutesOf(startTime) > shortest * MINUTES_PER_DAY) {
            throw new CalendarException.OccurrenceLongerThanInterval();
        }
        // Rare enough, an occurrence may last years without running into the next: a read could no longer show it whole.
        if (durationDays + 1L > MAX_EVENT_DAYS) {
            throw new CalendarException.EventTooLong(MAX_EVENT_DAYS);
        }
    }

    private static void validateRule(RecurrenceInterval intervalType, Set<DayOfWeek> weekdays, DayRule dayRule,
                                     LocalDate anchorDate) {
        boolean someDays = weekdays != null && !weekdays.isEmpty();
        if (someDays && (intervalType != RecurrenceInterval.WEEKLY || !weekdays.contains(anchorDate.getDayOfWeek()))) {
            throw new CalendarException.InvalidRecurrenceRule();
        }
        if (dayRule != null && dayRule != DayRule.SAME_DAY
            && (intervalType != RecurrenceInterval.MONTHLY && intervalType != RecurrenceInterval.YEARLY
                || !dayRule.fits(anchorDate))) {
            throw new CalendarException.InvalidRecurrenceRule();
        }
    }

    /**
     * The shortest time between two occurrences, in days: between the two closest weekdays chosen — across the end of a
     * period too —, a month at its shortest (28), a year at its shortest (365, or 364 for "the fourth Thursday of
     * November", which comes back after 52 weeks). An occurrence that fits then fits between any two of them.
     */
    static long shortestIntervalDays(RecurrenceInterval intervalType, int intervalCount,
                                     Set<DayOfWeek> weekdays, DayRule dayRule) {
        return switch (intervalType) {
            case DAILY -> intervalCount;
            case WEEKLY -> weeklyGaps(weekdays, intervalCount).min().orElseThrow();
            case MONTHLY -> 28L * intervalCount;
            case YEARLY -> (dayRule == DayRule.WEEKDAY_OF_MONTH || dayRule == DayRule.LAST_WEEKDAY_OF_MONTH ? 364L : 365L)
                * intervalCount;
        };
    }

    /**
     * The longest time between two occurrences in a row of {@code series}, in days, as far as a weekly series' days go:
     * between the two weekdays chosen furthest apart, across the end of a period too. A month or a year is still reckoned
     * at its shortest: no day between two of its occurrences lies that far from both.
     */
    static long longestIntervalDays(RecurringEventSeries series) {
        return series.intervalType() == RecurrenceInterval.WEEKLY
            ? weeklyGaps(series.weekdays(), series.intervalCount()).max().orElseThrow()
            : shortestIntervalDays(series.intervalType(), series.intervalCount(), series.weekdays(), series.dayRule());
    }

    /** The days between each weekday chosen and the next, the last of a period to the first of the next one included. */
    private static LongStream weeklyGaps(Set<DayOfWeek> weekdays, int intervalCount) {
        List<Integer> days = weekdays.stream().map(DayOfWeek::getValue).sorted().toList();
        return LongStream.concat(
            IntStream.range(1, days.size()).mapToLong(i -> days.get(i) - days.get(i - 1)),
            LongStream.of(7L * intervalCount - (days.getLast() - days.getFirst())));
    }

    private static long minutesOf(LocalTime time) {
        return time.getHour() * 60L + time.getMinute();
    }
}
