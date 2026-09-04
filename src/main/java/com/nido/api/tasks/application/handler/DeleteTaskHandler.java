package com.nido.api.tasks.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.application.port.in.DeleteTaskUseCase;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskException;
import com.nido.api.tasks.domain.port.out.RecurringTaskSeriesRepository;
import com.nido.api.tasks.domain.port.out.TaskRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class DeleteTaskHandler implements DeleteTaskUseCase {

    private final TaskRepository taskRepository;
    private final RecurringTaskSeriesRepository seriesRepository;

    public DeleteTaskHandler(TaskRepository taskRepository, RecurringTaskSeriesRepository seriesRepository) {
        this.taskRepository = taskRepository;
        this.seriesRepository = seriesRepository;
    }

    @Override
    @Transactional
    public void delete(UUID taskId, UUID spaceId, SpaceMembership caller) {
        caller.ensureSameSpace(spaceId);
        caller.ensureCanWrite();
        Task existing = taskRepository.findById(taskId).orElseThrow(TaskException.TaskNotFound::new);
        if (!existing.spaceId().equals(spaceId)) {
            throw new TaskException.TaskNotFound();
        }
        if (existing.recurringSeriesId() != null) {
            // The tasks.recurring_series_id foreign key is ON DELETE CASCADE
            // (see Task 9), so deleting the series also removes this task row.
            seriesRepository.deleteById(existing.recurringSeriesId());
        } else {
            taskRepository.delete(taskId);
        }
    }
}
