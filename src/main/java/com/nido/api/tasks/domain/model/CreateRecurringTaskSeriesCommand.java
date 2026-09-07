package com.nido.api.tasks.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CreateRecurringTaskSeriesCommand(
    UUID spaceId, String title, TaskPriority priority, List<String> subtaskTemplates,
    RecurrenceInterval intervalType, int intervalCount,
    RecurrenceInterval leadIntervalType, int leadIntervalCount,
    LocalDate anchorDate, LocalDate endDate, List<UUID> rotationMemberIds, UUID creatorUserId
) {
    public CreateRecurringTaskSeriesCommand {
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(subtaskTemplates, "subtaskTemplates");
        Objects.requireNonNull(intervalType, "intervalType");
        Objects.requireNonNull(leadIntervalType, "leadIntervalType");
        Objects.requireNonNull(anchorDate, "anchorDate");
        Objects.requireNonNull(rotationMemberIds, "rotationMemberIds");
        Objects.requireNonNull(creatorUserId, "creatorUserId");
        if (intervalCount < 1) {
            throw new IllegalArgumentException("intervalCount must be >= 1");
        }
        if (leadIntervalCount < 0) {
            throw new IllegalArgumentException("leadIntervalCount must be >= 0");
        }
    }
}
