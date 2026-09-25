package com.nido.api.calendar.infrastructure.web.dto;

import com.nido.api.calendar.domain.model.CalendarEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record EventResponse(
    UUID id, String title, String description, String location,
    boolean allDay, LocalDate startDate, LocalTime startTime, LocalDate endDate, LocalTime endTime,
    String color, List<UUID> participantIds,
    UUID recurringSeriesId, LocalDate recurringOriginalDate,
    UUID createdBy, Instant createdAt
) {
    public static EventResponse from(CalendarEvent e) {
        return new EventResponse(
            e.id(), e.title(), e.description(), e.location(),
            e.allDay(), e.startDate(), e.startTime(), e.endDate(), e.endTime(),
            e.color(), e.participantIds(), e.recurringSeriesId(), e.recurringOriginalDate(),
            e.createdBy(), e.createdAt());
    }
}
