package com.nido.api.calendar.domain.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record UpdateRecurringEventSeriesCommand(
    UUID seriesId, String title, String description, String location,
    boolean allDay, LocalTime startTime, LocalTime endTime, int durationDays, String color,
    RecurrenceInterval intervalType, int intervalCount, Set<DayOfWeek> weekdays, DayRule dayRule,
    LocalDate anchorDate, LocalDate endDate,
    List<UUID> participantIds
) {
    /** An edit on the anchor's own day, the same day of the month. */
    public UpdateRecurringEventSeriesCommand(
        UUID seriesId, String title, String description, String location,
        boolean allDay, LocalTime startTime, LocalTime endTime, int durationDays, String color,
        RecurrenceInterval intervalType, int intervalCount, LocalDate anchorDate, LocalDate endDate,
        List<UUID> participantIds) {
        this(seriesId, title, description, location, allDay, startTime, endTime, durationDays, color,
            intervalType, intervalCount, null, null, anchorDate, endDate, participantIds);
    }

    /** The same edit with the participants its space's rule settled on. */
    public UpdateRecurringEventSeriesCommand withParticipants(List<UUID> participants) {
        return new UpdateRecurringEventSeriesCommand(seriesId, title, description, location, allDay, startTime, endTime,
            durationDays, color, intervalType, intervalCount, weekdays, dayRule, anchorDate, endDate, participants);
    }
}
