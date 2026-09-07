package com.nido.api.tasks.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.application.port.in.DeleteTaskUseCase;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskException;
import com.nido.api.tasks.domain.port.out.TaskRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class DeleteTaskHandler implements DeleteTaskUseCase {

    private final TaskRepository taskRepository;

    public DeleteTaskHandler(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
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
        taskRepository.delete(taskId);
    }
}
