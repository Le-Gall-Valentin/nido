package com.nido.api.tasks.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.ResolveMembershipUseCase;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.application.port.in.MoveTaskUseCase;
import com.nido.api.tasks.domain.model.CreateTaskCommand;
import com.nido.api.tasks.domain.model.SubtaskInput;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskException;
import com.nido.api.tasks.domain.port.out.TaskRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationService
public class MoveTaskHandler implements MoveTaskUseCase {

    private final TaskRepository taskRepository;
    private final ResolveMembershipUseCase resolveMembershipUseCase;

    public MoveTaskHandler(TaskRepository taskRepository, ResolveMembershipUseCase resolveMembershipUseCase) {
        this.taskRepository = taskRepository;
        this.resolveMembershipUseCase = resolveMembershipUseCase;
    }

    @Override
    @Transactional
    public Task move(UUID taskId, UUID destinationSpaceId, SpaceMembership caller) {
        caller.ensureCanWrite();
        if (destinationSpaceId.equals(caller.spaceId())) {
            throw new TaskException.SameSpaceTransfer();
        }
        Task source = taskRepository.findById(taskId).orElseThrow(TaskException.TaskNotFound::new);
        if (!source.spaceId().equals(caller.spaceId())) {
            throw new TaskException.TaskNotFound();
        }
        SpaceMembership destination = resolveMembershipUseCase.resolve(destinationSpaceId, caller.userId());
        destination.ensureCanWrite();
        List<SubtaskInput> subtasks = source.subtasks().stream()
            .map(s -> new SubtaskInput(s.text(), s.done())).toList();
        Task moved = taskRepository.create(new CreateTaskCommand(
            destinationSpaceId, source.title(), source.priority(), source.dueDate(), List.of(), subtasks, null));
        taskRepository.delete(taskId);
        return moved;
    }
}
