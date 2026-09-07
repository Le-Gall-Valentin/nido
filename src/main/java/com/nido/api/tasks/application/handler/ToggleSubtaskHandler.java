package com.nido.api.tasks.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.application.port.in.ToggleSubtaskUseCase;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskException;
import com.nido.api.tasks.domain.port.out.TaskRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class ToggleSubtaskHandler implements ToggleSubtaskUseCase {

    private final TaskRepository taskRepository;

    public ToggleSubtaskHandler(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    @Transactional
    public void toggle(UUID taskId, UUID subtaskId, UUID spaceId, SpaceMembership caller) {
        caller.ensureSameSpace(spaceId);
        caller.ensureCanWrite();
        Task existing = taskRepository.findById(taskId).orElseThrow(TaskException.TaskNotFound::new);
        if (!existing.spaceId().equals(spaceId)) {
            throw new TaskException.TaskNotFound();
        }
        // Without this check, subtaskId is trusted on its own: any caller who can write to some
        // space could toggle an arbitrary subtask elsewhere — including in a space they aren't
        // even a member of — just by knowing its UUID, since taskRepository.toggleSubtask looks
        // the subtask up by its own id alone.
        if (existing.subtasks().stream().noneMatch(s -> s.id().equals(subtaskId))) {
            throw new TaskException.TaskNotFound();
        }
        taskRepository.toggleSubtask(taskId, subtaskId);
    }
}
