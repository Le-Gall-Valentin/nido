package com.nido.api.tasks.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.application.port.in.ChangeTaskStatusUseCase;
import com.nido.api.tasks.domain.model.CreateTaskCommand;
import com.nido.api.tasks.domain.model.RecurrenceScheduler;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.model.SubtaskInput;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskException;
import com.nido.api.tasks.domain.model.TaskStatus;
import com.nido.api.tasks.domain.port.out.RecurringTaskSeriesRepository;
import com.nido.api.tasks.domain.port.out.TaskRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationService
public class ChangeTaskStatusHandler implements ChangeTaskStatusUseCase {

    private final TaskRepository taskRepository;
    private final RecurringTaskSeriesRepository seriesRepository;

    public ChangeTaskStatusHandler(TaskRepository taskRepository, RecurringTaskSeriesRepository seriesRepository) {
        this.taskRepository = taskRepository;
        this.seriesRepository = seriesRepository;
    }

    @Override
    @Transactional
    public Task changeStatus(UUID taskId, UUID spaceId, TaskStatus newStatus, SpaceMembership caller) {
        caller.ensureSameSpace(spaceId);
        caller.ensureCanWrite();
        Task existing = taskRepository.findById(taskId).orElseThrow(TaskException.TaskNotFound::new);
        if (!existing.spaceId().equals(spaceId)) {
            throw new TaskException.TaskNotFound();
        }
        if (newStatus == TaskStatus.DONE && existing.subtasks().stream().anyMatch(s -> !s.done())) {
            throw new TaskException.SubtasksIncomplete();
        }
        Task updated = taskRepository.updateStatus(taskId, newStatus);
        if (newStatus == TaskStatus.DONE && existing.recurringSeriesId() != null) {
            generateNextOccurrence(existing.recurringSeriesId(), spaceId);
        }
        return updated;
    }

    private void generateNextOccurrence(UUID seriesId, UUID spaceId) {
        RecurringTaskSeries series = seriesRepository.findById(seriesId).orElseThrow(TaskException.TaskNotFound::new);
        LocalDate nextDueDate = RecurrenceScheduler.nextDueDate(
            series.anchorDate(), series.intervalType(), series.intervalCount(), series.occurrenceCount() + 1);
        List<UUID> nextAssignees = List.of();
        int nextRotationIndex = series.currentRotationIndex();
        if (!series.rotationMemberIds().isEmpty()) {
            nextRotationIndex = (series.currentRotationIndex() + 1) % series.rotationMemberIds().size();
            nextAssignees = List.of(series.rotationMemberIds().get(nextRotationIndex));
        }
        List<SubtaskInput> nextSubtasks = series.subtaskTemplates().stream()
            .map(text -> new SubtaskInput(text, false)).toList();
        taskRepository.create(new CreateTaskCommand(
            spaceId, series.title(), series.priority(), nextDueDate, nextAssignees, nextSubtasks, seriesId));
        seriesRepository.advance(seriesId, nextRotationIndex, series.occurrenceCount() + 1);
    }
}
