package com.nido.api.calendar.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Times are nullable on purpose: whether they are required depends on {@code allDay}, which bean
 * validation cannot express on its own. EventScheduleValidator enforces the pairing and turns a
 * mismatch into a 400 that names the problem.
 *
 * <p>{@code allDay} is boxed rather than primitive, and defaulted in its accessor. Jackson refuses
 * to map an absent JSON field onto a primitive record component, so a client omitting it would get
 * an opaque "Failed to read request" instead of a timed event. Same reasoning for participantIds.
 */
public record CreateEventRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 2000) String description,
    @Size(max = 200) String location,
    Boolean allDay,
    @NotNull LocalDate startDate,
    LocalTime startTime,
    @NotNull LocalDate endDate,
    LocalTime endTime,
    @Size(max = 30) String color,
    List<UUID> participantIds
) {
    public boolean allDayOrDefault() {
        return Boolean.TRUE.equals(allDay);
    }

    public List<UUID> participantIds() {
        return participantIds == null ? List.of() : participantIds;
    }
}
