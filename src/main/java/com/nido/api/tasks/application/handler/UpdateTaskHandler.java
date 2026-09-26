package com.nido.api.tasks.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.application.port.in.UpdateTaskUseCase;
import com.nido.api.tasks.application.service.TaskSpaceMemberValidator;
import com.nido.api.tasks.domain.model.Subtask;
import com.nido.api.tasks.domain.model.SubtaskEdit;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskException;
import com.nido.api.tasks.domain.model.TaskStatus;
import com.nido.api.tasks.domain.model.UpdateTaskCommand;
import com.nido.api.tasks.domain.port.out.TaskRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        if (command.subtasks() != null) {
            ensureOwnSubtasks(existing, command.subtasks());
        }
        Task updated = taskRepository.update(new UpdateTaskCommand(command.taskId(), command.spaceId(),
            command.title(), command.priority(), dueDateToSave(existing, command.dueDate()),
            command.assigneeIds(), command.subtasks()));
        if (existing.status() == TaskStatus.DONE && command.subtasks() != null
                && leavesAnUncheckedSubtask(existing, command.subtasks())) {
            // A task is done only once every subtask is: one that gains something left to do is
            // back in progress, which keeps the rule ChangeTaskStatusHandler enforces true.
            return taskRepository.updateStatus(command.taskId(), TaskStatus.DOING);
        }
        return updated;
    }

    /**
     * Every listed id must name a distinct subtask of this very task. The adapter finds a subtask
     * by its own id alone — a foreign one would be renamed and pulled into this list, wherever it
     * lives — and one listed twice has no single place to go.
     */
    private static void ensureOwnSubtasks(Task existing, List<SubtaskEdit> edits) {
        Set<UUID> own = existing.subtasks().stream().map(Subtask::id).collect(Collectors.toSet());
        Set<UUID> seen = new HashSet<>();
        for (SubtaskEdit edit : edits) {
            if (edit.id() != null && (!own.contains(edit.id()) || !seen.add(edit.id()))) {
                throw new TaskException.TaskNotFound();
            }
        }
    }

    /** A new subtask starts unchecked; a kept one keeps its check. */
    private static boolean leavesAnUncheckedSubtask(Task existing, List<SubtaskEdit> edits) {
        Map<UUID, Subtask> own = existing.subtasks().stream().collect(Collectors.toMap(Subtask::id, Function.identity()));
        return edits.stream().anyMatch(edit -> edit.id() == null || !own.get(edit.id()).done());
    }

    /**
     * A completed task never shows its due date (see TaskResponse), so a form editing one comes
     * back without it. That absence is not a request to erase a date the user never saw.
     */
    private static LocalDate dueDateToSave(Task existing, LocalDate sent) {
        return sent == null && existing.status() == TaskStatus.DONE ? existing.dueDate() : sent;
    }
}
