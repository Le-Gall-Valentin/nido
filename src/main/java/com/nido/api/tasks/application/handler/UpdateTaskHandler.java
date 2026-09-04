package com.nido.api.tasks.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.application.port.in.UpdateTaskUseCase;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskException;
import com.nido.api.tasks.domain.model.UpdateTaskCommand;
import com.nido.api.tasks.domain.port.out.TaskRepository;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class UpdateTaskHandler implements UpdateTaskUseCase {

    private final TaskRepository taskRepository;

    public UpdateTaskHandler(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    @Transactional
    public Task update(UpdateTaskCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        Task existing = taskRepository.findById(command.taskId()).orElseThrow(TaskException.TaskNotFound::new);
        if (!existing.spaceId().equals(command.spaceId())) {
            throw new TaskException.TaskNotFound();
        }
        return taskRepository.update(command);
    }
}
