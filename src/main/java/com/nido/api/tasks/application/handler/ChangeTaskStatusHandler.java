package com.nido.api.tasks.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.application.port.in.ChangeTaskStatusUseCase;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskException;
import com.nido.api.tasks.domain.model.TaskStatus;
import com.nido.api.tasks.domain.port.out.TaskRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class ChangeTaskStatusHandler implements ChangeTaskStatusUseCase {

    private final TaskRepository taskRepository;

    public ChangeTaskStatusHandler(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
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
        return taskRepository.updateStatus(taskId, newStatus);
    }
}
