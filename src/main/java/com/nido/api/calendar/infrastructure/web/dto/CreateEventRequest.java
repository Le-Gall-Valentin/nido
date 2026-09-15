package com.nido.api.calendar.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Times are nullable here on purpose: whether they are required depends on {@code allDay}, which
 * bean validation cannot express on its own. EventScheduleValidator enforces the pairing and
 * turns a mismatch into a 400 naming the problem.
 */
public record CreateEventRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 2000) String description,
    @Size(max = 200) String location,
    boolean allDay,
    @NotNull LocalDate startDate,
    LocalTime startTime,
    @NotNull LocalDate endDate,
    LocalTime endTime,
    @Size(max = 30) String color,
    List<UUID> participantIds
) {
    public List<UUID> participantIds() {
        return participantIds == null ? List.of() : participantIds;
    }
}
