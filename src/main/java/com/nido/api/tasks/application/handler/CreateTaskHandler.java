package com.nido.api.tasks.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.application.port.in.CreateTaskUseCase;
import com.nido.api.tasks.application.service.TaskSpaceMemberValidator;
import com.nido.api.tasks.domain.model.CreateTaskCommand;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.port.out.TaskRepository;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class CreateTaskHandler implements CreateTaskUseCase {

    private final TaskRepository taskRepository;
    private final TaskSpaceMemberValidator spaceMemberValidator;

    public CreateTaskHandler(TaskRepository taskRepository, TaskSpaceMemberValidator spaceMemberValidator) {
        this.taskRepository = taskRepository;
        this.spaceMemberValidator = spaceMemberValidator;
    }

    @Override
    @Transactional
    public Task create(CreateTaskCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        command.assigneeIds().forEach(memberId -> spaceMemberValidator.ensureMember(command.spaceId(), memberId));
        return taskRepository.create(command);
    }
}
