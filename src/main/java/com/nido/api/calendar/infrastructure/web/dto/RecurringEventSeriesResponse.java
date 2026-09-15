package com.nido.api.calendar.infrastructure.web.dto;

import com.nido.api.calendar.domain.model.RecurrenceInterval;
import com.nido.api.calendar.domain.model.RecurringEventSeries;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record RecurringEventSeriesResponse(
    UUID id, String title, String description, String location,
    boolean allDay, LocalTime startTime, LocalTime endTime, int durationDays, String color,
    RecurrenceInterval intervalType, int intervalCount, LocalDate anchorDate, LocalDate endDate,
    List<UUID> participantIds, UUID createdBy, Instant createdAt
) {
    public static RecurringEventSeriesResponse from(RecurringEventSeries s) {
        return new RecurringEventSeriesResponse(
            s.id(), s.title(), s.description(), s.location(),
            s.allDay(), s.startTime(), s.endTime(), s.durationDays(), s.color(),
            s.intervalType(), s.intervalCount(), s.anchorDate(), s.endDate(),
            s.participantIds(), s.createdBy(), s.createdAt());
    }
}
