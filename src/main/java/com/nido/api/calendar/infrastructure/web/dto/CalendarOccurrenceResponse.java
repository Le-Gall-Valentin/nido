package com.nido.api.calendar.infrastructure.web.dto;

import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.calendar.domain.model.CalendarSourceType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * {@code sourceId} is a String, not a UUID: a projected occurrence has no row and so no id of its
 * own — it is keyed by its series and slot as "seriesId:date". Clients treat it as opaque.
 *
 * <p>{@code materialized} false means nothing in the database corresponds to this entry, which is
 * what tells a client that deleting it excludes a slot rather than deleting a row, and that it
 * cannot simply be dragged to another date.
 */
public record CalendarOccurrenceResponse(
    CalendarSourceType source, String sourceId, UUID seriesId, LocalDate originalDate,
    boolean materialized, String title, String description, String location, boolean allDay,
    LocalDate startDate, LocalTime startTime, LocalDate endDate, LocalTime endTime,
    String color, List<UUID> participantIds
) {
    public static CalendarOccurrenceResponse from(CalendarOccurrence o) {
        return new CalendarOccurrenceResponse(
            o.source(), o.sourceId(), o.seriesId(), o.originalDate(), o.materialized(),
            o.title(), o.description(), o.location(), o.allDay(),
            o.startDate(), o.startTime(), o.endDate(), o.endTime(),
            o.color(), o.participantIds());
    }
}
