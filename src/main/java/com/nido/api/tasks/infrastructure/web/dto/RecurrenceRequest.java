package com.nido.api.tasks.infrastructure.web.dto;

import com.nido.api.tasks.domain.model.RecurrenceInterval;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RecurrenceRequest(
    @NotNull RecurrenceInterval intervalType, @Min(1) int intervalCount,
    @NotNull RecurrenceInterval leadIntervalType, @Min(0) int leadIntervalCount,
    @NotNull LocalDate anchorDate, LocalDate endDate, List<UUID> rotationMemberIds
) {
    public List<UUID> rotationMemberIds() {
        return rotationMemberIds == null ? List.of() : rotationMemberIds;
    }
}
