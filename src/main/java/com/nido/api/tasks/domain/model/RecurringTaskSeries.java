package com.nido.api.tasks.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record RecurringTaskSeries(
    UUID id, UUID spaceId, String title, TaskPriority priority, List<String> subtaskTemplates,
    RecurrenceInterval intervalType, int intervalCount, LocalDate anchorDate,
    int occurrenceCount, List<UUID> rotationMemberIds, int currentRotationIndex
) {
    public RecurringTaskSeries {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(subtaskTemplates, "subtaskTemplates");
        Objects.requireNonNull(intervalType, "intervalType");
        Objects.requireNonNull(anchorDate, "anchorDate");
        Objects.requireNonNull(rotationMemberIds, "rotationMemberIds");
    }
}
