package com.nido.api.tasks.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.application.port.in.UpdateTaskUseCase;
import com.nido.api.tasks.application.service.TaskSpaceMemberValidator;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskException;
import com.nido.api.tasks.domain.model.UpdateTaskCommand;
import com.nido.api.tasks.domain.port.out.TaskRepository;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class UpdateTaskHandler implements UpdateTaskUseCase {

    private final TaskRepository taskRepository;
    private final TaskSpaceMemberValidator spaceMemberValidator;

    public UpdateTaskHandler(TaskRepository taskRepository, TaskSpaceMemberValidator spaceMemberValidator) {
        this.taskRepository = taskRepository;
        this.spaceMemberValidator = spaceMemberValidator;
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
        command.assigneeIds().forEach(memberId -> spaceMemberValidator.ensureMember(command.spaceId(), memberId));
        return taskRepository.update(command);
    }
}
