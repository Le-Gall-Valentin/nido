package com.nido.api.calendar.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * One entry of the unified feed, whatever produced it.
 *
 * <p>{@code sourceId} is a String rather than a UUID because a projected occurrence has no row
 * and therefore no id of its own: it is identified by its series and its slot, as
 * {@code "<seriesId>:<date>"}. Materialized items carry their real id. Consumers treat it as an
 * opaque key and never parse it.
 *
 * <p>{@code materialized} is false exactly when nothing in the database corresponds to this
 * entry, which is what tells a client that deleting it means excluding a slot rather than
 * deleting a row, and that it cannot simply be dragged to another date.
 */
public record CalendarOccurrence(
    CalendarSourceType source, String sourceId, UUID seriesId, LocalDate originalDate,
    boolean materialized, String title, boolean allDay,
    LocalDate startDate, LocalTime startTime, LocalDate endDate, LocalTime endTime,
    String color, List<UUID> participantIds
) {
    public static String projectedId(UUID seriesId, LocalDate slot) {
        return seriesId + ":" + slot;
    }
}
