package com.nido.api.calendar.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

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
        // An occurrence never runs into the next one, as in any diary. It is also what bounds a read:
        // every occurrence overlapping a window then starts at most one interval before it.
        if (allDay
            ? durationDays + 1L > shortestIntervalDays(intervalType, intervalCount)
            : durationDays * MINUTES_PER_DAY + minutesOf(endTime) - minutesOf(startTime)
                > shortestIntervalDays(intervalType, intervalCount) * MINUTES_PER_DAY) {
            throw new CalendarException.OccurrenceLongerThanInterval();
        }
    }

    /**
     * The shortest time between two occurrences, in days. A month counts as 28 days and a year as
     * 365, their shortest: an occurrence that fits then fits between any two of them, February's included.
     */
    static long shortestIntervalDays(RecurrenceInterval intervalType, int intervalCount) {
        long unit = switch (intervalType) {
            case DAILY -> 1;
            case WEEKLY -> 7;
            case MONTHLY -> 28;
            case YEARLY -> 365;
        };
        return unit * intervalCount;
    }

    private static long minutesOf(LocalTime time) {
        return time.getHour() * 60L + time.getMinute();
    }
}
