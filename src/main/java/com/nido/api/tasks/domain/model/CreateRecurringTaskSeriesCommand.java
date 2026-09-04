package com.nido.api.tasks.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CreateRecurringTaskSeriesCommand(
    UUID spaceId, String title, TaskPriority priority, List<String> subtaskTemplates,
    RecurrenceInterval intervalType, int intervalCount, LocalDate anchorDate, List<UUID> rotationMemberIds
) {
    public CreateRecurringTaskSeriesCommand {
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(subtaskTemplates, "subtaskTemplates");
        Objects.requireNonNull(intervalType, "intervalType");
        Objects.requireNonNull(anchorDate, "anchorDate");
        Objects.requireNonNull(rotationMemberIds, "rotationMemberIds");
        if (intervalCount < 1) {
            throw new IllegalArgumentException("intervalCount must be >= 1");
        }
    }
}
