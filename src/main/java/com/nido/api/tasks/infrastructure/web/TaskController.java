package com.nido.api.tasks.infrastructure.web;

import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.infrastructure.web.CurrentMembership;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.application.port.in.ChangeTaskStatusUseCase;
import com.nido.api.tasks.application.port.in.CreateRecurringTaskUseCase;
import com.nido.api.tasks.application.port.in.CreateTaskUseCase;
import com.nido.api.tasks.application.port.in.DeleteTaskUseCase;
import com.nido.api.tasks.application.port.in.ListTasksUseCase;
import com.nido.api.tasks.application.port.in.MoveTaskUseCase;
import com.nido.api.tasks.application.port.in.ToggleSubtaskUseCase;
import com.nido.api.tasks.application.port.in.UpdateTaskUseCase;
import com.nido.api.tasks.domain.model.CreateRecurringTaskSeriesCommand;
import com.nido.api.tasks.domain.model.CreateTaskCommand;
import com.nido.api.tasks.domain.model.SubtaskInput;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.UpdateTaskCommand;
import com.nido.api.tasks.infrastructure.web.dto.ChangeTaskStatusRequest;
import com.nido.api.tasks.infrastructure.web.dto.CreateTaskRequest;
import com.nido.api.tasks.infrastructure.web.dto.MoveTaskRequest;
import com.nido.api.tasks.infrastructure.web.dto.TaskResponse;
import com.nido.api.tasks.infrastructure.web.dto.UpdateTaskRequest;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/spaces/{spaceId}/tasks")
@Validated
@Tag(name = "Tâches", description = "Tâches d'un contexte")
public class TaskController {

    private final ListTasksUseCase listTasksUseCase;
    private final CreateTaskUseCase createTaskUseCase;
    private final CreateRecurringTaskUseCase createRecurringTaskUseCase;
    private final UpdateTaskUseCase updateTaskUseCase;
    private final ChangeTaskStatusUseCase changeTaskStatusUseCase;
    private final ToggleSubtaskUseCase toggleSubtaskUseCase;
    private final DeleteTaskUseCase deleteTaskUseCase;
    private final MoveTaskUseCase moveTaskUseCase;

    public TaskController(ListTasksUseCase listTasksUseCase,
                           CreateTaskUseCase createTaskUseCase,
                           CreateRecurringTaskUseCase createRecurringTaskUseCase,
                           UpdateTaskUseCase updateTaskUseCase,
                           ChangeTaskStatusUseCase changeTaskStatusUseCase,
                           ToggleSubtaskUseCase toggleSubtaskUseCase,
                           DeleteTaskUseCase deleteTaskUseCase,
                           MoveTaskUseCase moveTaskUseCase) {
        this.listTasksUseCase = listTasksUseCase;
        this.createTaskUseCase = createTaskUseCase;
        this.createRecurringTaskUseCase = createRecurringTaskUseCase;
        this.updateTaskUseCase = updateTaskUseCase;
        this.changeTaskStatusUseCase = changeTaskStatusUseCase;
        this.toggleSubtaskUseCase = toggleSubtaskUseCase;
        this.deleteTaskUseCase = deleteTaskUseCase;
        this.moveTaskUseCase = moveTaskUseCase;
    }

    @GetMapping
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TaskResponse>> list(
            @PathVariable UUID spaceId, @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        return ResponseEntity.ok(listTasksUseCase.list(membership).stream().map(TaskResponse::from).toList());
    }

    @PostMapping
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskResponse> create(
            @PathVariable UUID spaceId, @Valid @RequestBody CreateTaskRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        Task created;
        if (request.recurrence() != null) {
            created = createRecurringTaskUseCase.create(new CreateRecurringTaskSeriesCommand(
                spaceId, request.title(), request.priority(), request.subtasks(),
                request.recurrence().intervalType(), request.recurrence().intervalCount(),
                request.recurrence().anchorDate(), request.recurrence().rotationMemberIds()), membership);
        } else {
            List<SubtaskInput> subtasks = request.subtasks().stream().map(text -> new SubtaskInput(text, false)).toList();
            created = createTaskUseCase.create(new CreateTaskCommand(
                spaceId, request.title(), request.priority(), request.dueDate(),
                request.assigneeIds(), subtasks, null), membership);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(created));
    }

    @PatchMapping("/{taskId}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskResponse> update(
            @PathVariable UUID spaceId, @PathVariable UUID taskId, @Valid @RequestBody UpdateTaskRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        Task updated = updateTaskUseCase.update(new UpdateTaskCommand(
            taskId, spaceId, request.title(), request.priority(), request.dueDate(), request.assigneeIds()), membership);
        return ResponseEntity.ok(TaskResponse.from(updated));
    }

    @PostMapping("/{taskId}/status")
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskResponse> changeStatus(
            @PathVariable UUID spaceId, @PathVariable UUID taskId, @Valid @RequestBody ChangeTaskStatusRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        Task updated = changeTaskStatusUseCase.changeStatus(taskId, spaceId, request.status(), membership);
        return ResponseEntity.ok(TaskResponse.from(updated));
    }

    @PostMapping("/{taskId}/subtasks/{subtaskId}/toggle")
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> toggleSubtask(
            @PathVariable UUID spaceId, @PathVariable UUID taskId, @PathVariable UUID subtaskId,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        toggleSubtaskUseCase.toggle(taskId, subtaskId, spaceId, membership);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{taskId}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable UUID spaceId, @PathVariable UUID taskId,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        deleteTaskUseCase.delete(taskId, spaceId, membership);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{taskId}/move")
    @RateLimiting(max = 20)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskResponse> move(
            @PathVariable UUID spaceId, @PathVariable UUID taskId, @Valid @RequestBody MoveTaskRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        Task moved = moveTaskUseCase.move(taskId, request.destinationSpaceId(), membership);
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(moved));
    }
}
