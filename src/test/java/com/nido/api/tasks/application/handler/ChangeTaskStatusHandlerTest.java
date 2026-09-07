package com.nido.api.tasks.application.handler;

import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.tasks.domain.model.Subtask;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskException;
import com.nido.api.tasks.domain.model.TaskPriority;
import com.nido.api.tasks.domain.model.TaskStatus;
import com.nido.api.tasks.domain.port.out.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeTaskStatusHandlerTest {

    @Mock TaskRepository taskRepository;
    private ChangeTaskStatusHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID taskId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ChangeTaskStatusHandler(taskRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    private Task oneOffTask(TaskStatus status, List<Subtask> subtasks) {
        return new Task(taskId, spaceId, "T", status, TaskPriority.MED, null, List.of(), subtasks, null, null, Instant.now());
    }

    @Test
    void a_member_can_move_a_task_between_non_terminal_statuses() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(oneOffTask(TaskStatus.TODO, List.of())));
        Task updated = oneOffTask(TaskStatus.DOING, List.of());
        when(taskRepository.updateStatus(taskId, TaskStatus.DOING)).thenReturn(updated);

        Task result = handler.changeStatus(taskId, spaceId, TaskStatus.DOING, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(updated);
    }

    @Test
    void a_viewer_cannot_change_status() {
        assertThatThrownBy(() -> handler.changeStatus(taskId, spaceId, TaskStatus.DOING, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }

    @Test
    void a_task_from_another_space_is_not_found() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(
            new Task(taskId, UUID.randomUUID(), "T", TaskStatus.TODO, TaskPriority.MED, null, List.of(), List.of(), null, null, Instant.now())));

        assertThatThrownBy(() -> handler.changeStatus(taskId, spaceId, TaskStatus.DOING, membership(SpaceRole.MEMBER)))
            .isInstanceOf(TaskException.TaskNotFound.class);
    }

    @Test
    void completing_a_task_with_an_open_subtask_is_rejected_before_anything_else_happens() {
        Subtask open = new Subtask(UUID.randomUUID(), "Vérifier", false);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(oneOffTask(TaskStatus.DOING, List.of(open))));

        assertThatThrownBy(() -> handler.changeStatus(taskId, spaceId, TaskStatus.DONE, membership(SpaceRole.MEMBER)))
            .isInstanceOf(TaskException.SubtasksIncomplete.class);
        verify(taskRepository, never()).updateStatus(taskId, TaskStatus.DONE);
    }

    @Test
    void completing_a_recurring_occurrence_does_not_generate_a_next_occurrence() {
        Task recurring = new Task(taskId, spaceId, "T", TaskStatus.DOING, TaskPriority.MED, LocalDate.of(2026, 1, 7), List.of(), List.of(),
            UUID.randomUUID(), null, Instant.now());
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(recurring));
        Task done = new Task(taskId, spaceId, "T", TaskStatus.DONE, TaskPriority.MED, recurring.dueDate(), List.of(), List.of(),
            recurring.recurringSeriesId(), null, Instant.now());
        when(taskRepository.updateStatus(taskId, TaskStatus.DONE)).thenReturn(done);

        Task result = handler.changeStatus(taskId, spaceId, TaskStatus.DONE, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(done);
        verify(taskRepository, never()).create(any());
    }
}
