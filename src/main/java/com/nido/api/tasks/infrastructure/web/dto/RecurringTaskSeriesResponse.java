package com.nido.api.tasks.infrastructure.web.dto;

import com.nido.api.tasks.domain.model.RecurrenceInterval;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.model.TaskPriority;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RecurringTaskSeriesResponse(
    UUID id, String title, TaskPriority priority, List<String> subtaskTemplates,
    RecurrenceInterval intervalType, int intervalCount,
    RecurrenceInterval leadIntervalType, int leadIntervalCount,
    LocalDate anchorDate, LocalDate endDate, List<UUID> rotationMemberIds
) {
    public static RecurringTaskSeriesResponse from(RecurringTaskSeries s) {
        return new RecurringTaskSeriesResponse(s.id(), s.title(), s.priority(), s.subtaskTemplates(),
            s.intervalType(), s.intervalCount(), s.leadIntervalType(), s.leadIntervalCount(),
            s.anchorDate(), s.endDate(), s.rotationMemberIds());
    }
}
