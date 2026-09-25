package com.nido.api.calendar.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record UpdateRecurringEventSeriesCommand(
    UUID seriesId, String title, String description, String location,
    boolean allDay, LocalTime startTime, LocalTime endTime, int durationDays, String color,
    RecurrenceInterval intervalType, int intervalCount, LocalDate anchorDate, LocalDate endDate,
    List<UUID> participantIds
) {
    /** The same edit with the participants its space's rule settled on. */
    public UpdateRecurringEventSeriesCommand withParticipants(List<UUID> participants) {
        return new UpdateRecurringEventSeriesCommand(seriesId, title, description, location, allDay, startTime, endTime,
            durationDays, color, intervalType, intervalCount, anchorDate, endDate, participants);
    }
}
