package com.nido.api.calendar.infrastructure.web.dto;

import com.nido.api.calendar.domain.model.RecurrenceInterval;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * {@code durationDays} is how many days past its start each occurrence runs — 0 for a same-day
 * event, 2 for a long weekend. It is how a recurring multi-day event is expressed, since nothing
 * is materialized and there are no per-occurrence end dates to carry.
 */
public record RecurringEventSeriesRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 2000) String description,
    @Size(max = 200) String location,
    Boolean allDay,
    LocalTime startTime,
    LocalTime endTime,
    @Min(0) Integer durationDays,
    @Size(max = 30) String color,
    @NotNull RecurrenceInterval intervalType,
    @NotNull @Min(1) Integer intervalCount,
    @NotNull LocalDate anchorDate,
    LocalDate endDate,
    List<UUID> participantIds
) {
    public boolean allDayOrDefault() {
        return Boolean.TRUE.equals(allDay);
    }

    /** Absent means a same-day occurrence, which is the common case and must not need spelling out. */
    public int durationDaysOrDefault() {
        return durationDays == null ? 0 : durationDays;
    }

    public List<UUID> participantIds() {
        return participantIds == null ? List.of() : participantIds;
    }
}
