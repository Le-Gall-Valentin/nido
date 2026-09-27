package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.calendar.domain.model.EventRecurrenceProjector;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

/** The window rules and the order every windowed calendar read shares, written once. */
final class OccurrenceWindow {

    /** All-day entries sort before timed ones on the same day, then by time, then by title. */
    static final Comparator<CalendarOccurrence> ORDER =
        Comparator.comparing(CalendarOccurrence::startDate)
            .thenComparing(o -> o.startTime() == null ? LocalTime.MIN : o.startTime())
            .thenComparing(CalendarOccurrence::title);

    private OccurrenceWindow() {}

    static void validate(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new CalendarException.ReversedWindow();
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days > EventRecurrenceProjector.MAX_WINDOW_DAYS) {
            throw new CalendarException.WindowTooLarge((int) Math.min(days, Integer.MAX_VALUE),
                EventRecurrenceProjector.MAX_WINDOW_DAYS);
        }
    }
}
