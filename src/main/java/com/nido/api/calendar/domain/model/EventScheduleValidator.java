package com.nido.api.calendar.domain.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

/**
 * Validates a command's schedule <em>before</em> it reaches the database.
 *
 * <p>The same rules are carried by the records and by the table's CHECK constraints, but neither
 * is reachable in time: the record is only built from the row after the insert, and letting the
 * constraint fire turns a user mistake into a DataIntegrityViolationException, which surfaces as
 * a 500 rather than a 400 naming what is wrong.
 */
public final class EventScheduleValidator {

    /** The most days an event may cover: no more than a single calendar read can show. */
    public static final int MAX_EVENT_DAYS = EventRecurrenceProjector.MAX_WINDOW_DAYS;

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
        validateRule(intervalType, weekdays, dayRule, anchorDate);
        // An occurrence never runs into the next one, as in any diary. It is also what bounds a read:
        // every occurrence overlapping a window then starts at most one interval before it.
        long shortest = shortestIntervalDays(intervalType, intervalCount,
            intervalType == RecurrenceInterval.WEEKLY && (weekdays == null || weekdays.isEmpty())
                ? Weekdays.of(anchorDate.getDayOfWeek()) : weekdays,
            dayRule == null ? DayRule.SAME_DAY : dayRule);
        if (allDay
            ? durationDays + 1L > shortest
            : durationDays * MINUTES_PER_DAY + minutesOf(endTime) - minutesOf(startTime) > shortest * MINUTES_PER_DAY) {
            throw new CalendarException.OccurrenceLongerThanInterval();
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
            case WEEKLY -> shortestWeeklyGap(weekdays, intervalCount);
            case MONTHLY -> 28L * intervalCount;
            case YEARLY -> (dayRule == DayRule.WEEKDAY_OF_MONTH || dayRule == DayRule.LAST_WEEKDAY_OF_MONTH ? 364L : 365L)
                * intervalCount;
        };
    }

    /** The shortest time between two occurrences of {@code series}, in days. */
    static long shortestIntervalDays(RecurringEventSeries series) {
        return shortestIntervalDays(series.intervalType(), series.intervalCount(), series.weekdays(), series.dayRule());
    }

    private static long shortestWeeklyGap(Set<DayOfWeek> weekdays, int intervalCount) {
        List<Integer> days = weekdays.stream().map(DayOfWeek::getValue).sorted().toList();
        long shortest = 7L * intervalCount - (days.getLast() - days.getFirst());
        for (int i = 1; i < days.size(); i++) {
            shortest = Math.min(shortest, days.get(i) - days.get(i - 1));
        }
        return shortest;
    }

    private static long minutesOf(LocalTime time) {
        return time.getHour() * 60L + time.getMinute();
    }
}
