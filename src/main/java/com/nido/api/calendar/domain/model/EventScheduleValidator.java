package com.nido.api.calendar.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Validates a command's schedule <em>before</em> it reaches the database.
 *
 * <p>The same rules are carried by the records and by the table's CHECK constraints, but neither
 * is reachable in time: the record is only built from the row after the insert, and letting the
 * constraint fire turns a user mistake into a DataIntegrityViolationException, which surfaces as
 * a 500 rather than a 400 naming what is wrong.
 */
public final class EventScheduleValidator {

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
    }

    public static void validateSeries(boolean allDay, LocalTime startTime, LocalTime endTime,
                                      int durationDays, LocalDate anchorDate, LocalDate endDate) {
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
    }
}
