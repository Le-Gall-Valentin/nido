package com.nido.api.tasks.application.handler;

import com.nido.api.tasks.domain.model.CreateTaskCommand;
import com.nido.api.tasks.domain.model.RecurrenceScheduler;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.model.SubtaskInput;
import com.nido.api.tasks.domain.port.out.RecurringTaskSeriesRepository;
import com.nido.api.tasks.domain.port.out.TaskRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Lazily materializes any recurring task occurrence whose lead-time window has
 * opened — shared by the tasks module's only read path ({@code ListTasksHandler})
 * so a series never silently misses an occurrence. Unlike finance's equivalent
 * materializer, "due" here means the occurrence's due date minus its lead time is
 * on or before today, not the due date itself — that's the whole point of the
 * lead-time feature: a task can appear before its actual deadline.
 */
final class RecurringTaskSeriesMaterializer {

    private RecurringTaskSeriesMaterializer() {}

    static void materializeDueOccurrences(
            TaskRepository taskRepository, RecurringTaskSeriesRepository seriesRepository, UUID spaceId, LocalDate today) {
        seriesRepository.lockForMaterialization(spaceId);
        for (RecurringTaskSeries series : seriesRepository.findBySpaceId(spaceId)) {
            int occurrence = series.occurrenceCount() + 1;
            int rotationIndex = series.currentRotationIndex();
            int lastGenerated = series.occurrenceCount();
            while (true) {
                LocalDate dueDate = RecurrenceScheduler.nextDueDate(
                    series.anchorDate(), series.intervalType(), series.intervalCount(), occurrence);
                if (series.endDate() != null && dueDate.isAfter(series.endDate())) {
                    break;
                }
                LocalDate windowOpensOn = RecurrenceScheduler.minus(dueDate, series.leadIntervalType(), series.leadIntervalCount());
                if (windowOpensOn.isAfter(today)) {
                    break;
                }
                List<UUID> assignees = List.of();
                if (!series.rotationMemberIds().isEmpty()) {
                    rotationIndex = (rotationIndex + 1) % series.rotationMemberIds().size();
                    assignees = List.of(series.rotationMemberIds().get(rotationIndex));
                }
                List<SubtaskInput> subtasks = series.subtaskTemplates().stream()
                    .map(text -> new SubtaskInput(text, false)).toList();
                taskRepository.create(new CreateTaskCommand(
                    spaceId, series.title(), series.priority(), dueDate, assignees, subtasks, series.id()));
                lastGenerated = occurrence;
                occurrence++;
            }
            if (lastGenerated != series.occurrenceCount()) {
                seriesRepository.advance(series.id(), rotationIndex, lastGenerated);
            }
        }
    }
}
