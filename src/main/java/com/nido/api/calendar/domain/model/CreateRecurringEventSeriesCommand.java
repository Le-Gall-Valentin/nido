package com.nido.api.calendar.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * {@code startsOn} is null for a new series, which shows from its anchor. It is set only for a series
 * carrying on an edit from the day it was made — see {@link RecurringEventSeries}.
 */
public record CreateRecurringEventSeriesCommand(
    UUID spaceId, String title, String description, String location,
    boolean allDay, LocalTime startTime, LocalTime endTime, int durationDays, String color,
    RecurrenceInterval intervalType, int intervalCount, LocalDate anchorDate, LocalDate endDate,
    List<UUID> participantIds, UUID createdBy, LocalDate startsOn
) {
    /** A new series, shown from its anchor. */
    public CreateRecurringEventSeriesCommand(
        UUID spaceId, String title, String description, String location,
        boolean allDay, LocalTime startTime, LocalTime endTime, int durationDays, String color,
        RecurrenceInterval intervalType, int intervalCount, LocalDate anchorDate, LocalDate endDate,
        List<UUID> participantIds, UUID createdBy) {
        this(spaceId, title, description, location, allDay, startTime, endTime, durationDays, color,
            intervalType, intervalCount, anchorDate, endDate, participantIds, createdBy, null);
    }

    /** The same series with the participants its space's rule settled on. */
    public CreateRecurringEventSeriesCommand withParticipants(List<UUID> participants) {
        return new CreateRecurringEventSeriesCommand(spaceId, title, description, location, allDay, startTime, endTime,
            durationDays, color, intervalType, intervalCount, anchorDate, endDate, participants, createdBy, startsOn);
    }
}
