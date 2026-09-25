package com.nido.api.calendar.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The template of a recurring event.
 *
 * <p>{@code durationDays} is how many days past its start each occurrence runs — 0 for a
 * same-day event. Storing a duration rather than an end date per occurrence is what lets a
 * recurring multi-day event exist at all: the series has no occurrences of its own to carry
 * end dates on, since nothing is ever materialized.
 *
 * <p>{@code anchorDate} sets the rhythm; {@code startsOn}, when set, is the first day the series
 * shows. They differ once a series has been edited after it began: the edit carries on from that day
 * in a series of its own, keeping the rhythm — "the 31st of each month" stays the 31st even when it
 * picks up in February — while the occurrences before it stay with the series as it was.
 */
public record RecurringEventSeries(
    UUID id, UUID spaceId, String title, String description, String location,
    boolean allDay, LocalTime startTime, LocalTime endTime, int durationDays, String color,
    RecurrenceInterval intervalType, int intervalCount,
    LocalDate anchorDate, LocalDate endDate, LocalDate startsOn,
    List<UUID> participantIds, UUID createdBy, Instant createdAt
) {
    /** A series shown from its anchor on — every series that was never carried on from another. */
    public RecurringEventSeries(
        UUID id, UUID spaceId, String title, String description, String location,
        boolean allDay, LocalTime startTime, LocalTime endTime, int durationDays, String color,
        RecurrenceInterval intervalType, int intervalCount,
        LocalDate anchorDate, LocalDate endDate,
        List<UUID> participantIds, UUID createdBy, Instant createdAt) {
        this(id, spaceId, title, description, location, allDay, startTime, endTime, durationDays, color,
            intervalType, intervalCount, anchorDate, endDate, null, participantIds, createdBy, createdAt);
    }

    /** The first day the series shows an occurrence on or after. */
    public LocalDate shownFrom() {
        return startsOn != null && startsOn.isAfter(anchorDate) ? startsOn : anchorDate;
    }

    public RecurringEventSeries {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(intervalType, "intervalType");
        Objects.requireNonNull(anchorDate, "anchorDate");
        Objects.requireNonNull(participantIds, "participantIds");
        Objects.requireNonNull(createdAt, "createdAt");
        if (intervalCount < 1) {
            throw new IllegalArgumentException("intervalCount must be at least 1, got " + intervalCount);
        }
        if (durationDays < 0) {
            throw new IllegalArgumentException("durationDays must not be negative, got " + durationDays);
        }
        if (allDay != (startTime == null)) {
            throw new IllegalArgumentException("allDay and startTime disagree");
        }
        if ((startTime == null) != (endTime == null)) {
            throw new IllegalArgumentException("startTime and endTime must both be set or both be null");
        }
        if (endDate != null && endDate.isBefore(anchorDate)) {
            throw new IllegalArgumentException("endDate is before anchorDate");
        }
    }
}
