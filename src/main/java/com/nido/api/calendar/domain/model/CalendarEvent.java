package com.nido.api.calendar.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A one-off event, or an occurrence of a series that was edited on its own.
 *
 * <p>The nullable pair {@code recurringSeriesId} / {@code recurringOriginalDate} is what makes
 * the second case work: it records which slot of which series this row takes over, so the
 * projector skips that slot instead of emitting it alongside this row. Because a detached
 * occurrence is an ordinary row here, editing one and editing a one-off event are the same
 * code path — there is no parallel "occurrence override" shape to keep in step.
 *
 * <p>{@code recurringOriginalDate} is the slot replaced, never where the event now sits. Moving
 * this week's session from Tuesday to Thursday changes {@code startDate} and leaves the original
 * date alone, which is precisely what keeps Tuesday from reappearing.
 */
public record CalendarEvent(
    UUID id, UUID spaceId, String title, String description, String location,
    boolean allDay, LocalDate startDate, LocalTime startTime, LocalDate endDate, LocalTime endTime,
    String color, List<UUID> participantIds,
    UUID recurringSeriesId, LocalDate recurringOriginalDate,
    UUID createdBy, Instant createdAt
) {
    public CalendarEvent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");
        Objects.requireNonNull(participantIds, "participantIds");
        Objects.requireNonNull(createdAt, "createdAt");
        if (allDay != (startTime == null)) {
            throw new IllegalArgumentException("allDay and startTime disagree");
        }
        if ((startTime == null) != (endTime == null)) {
            throw new IllegalArgumentException("startTime and endTime must both be set or both be null");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate is before startDate");
        }
        if ((recurringSeriesId == null) != (recurringOriginalDate == null)) {
            throw new IllegalArgumentException("a detachment needs both a series and an original date");
        }
    }

    public boolean isDetachedOccurrence() {
        return recurringSeriesId != null;
    }
}
