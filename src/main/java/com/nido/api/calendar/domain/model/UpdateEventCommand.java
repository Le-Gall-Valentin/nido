package com.nido.api.calendar.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** The detachment pair is never updatable: a row's slot is decided when it is created. */
public record UpdateEventCommand(
    UUID eventId, String title, String description, String location,
    boolean allDay, LocalDate startDate, LocalTime startTime, LocalDate endDate, LocalTime endTime,
    String color, List<UUID> participantIds
) {}
