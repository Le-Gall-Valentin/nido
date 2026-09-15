package com.nido.api.calendar.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * {@code recurringSeriesId} / {@code recurringOriginalDate} are non-null only when this event
 * is being created as a detached occurrence of a series; a plain event leaves both null.
 */
public record CreateEventCommand(
    UUID spaceId, String title, String description, String location,
    boolean allDay, LocalDate startDate, LocalTime startTime, LocalDate endDate, LocalTime endTime,
    String color, List<UUID> participantIds,
    UUID recurringSeriesId, LocalDate recurringOriginalDate, UUID createdBy
) {}
