package com.nido.api.tasks.application.handler;

import com.nido.api.space.application.port.in.ResolveMembershipUseCase;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.tasks.domain.model.CreateTaskCommand;
import com.nido.api.tasks.domain.model.Subtask;
import com.nido.api.tasks.domain.model.SubtaskInput;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoveTaskHandlerTest {

    @Mock TaskRepository taskRepository;
    @Mock ResolveMembershipUseCase resolveMembershipUseCase;
    private MoveTaskHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID destinationSpaceId = UUID.randomUUID();
    private final UUID taskId = UUID.randomUUID();
    private final UUID callerUserId = UUID.randomUUID();
    private final UUID seriesId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new MoveTaskHandler(taskRepository, resolveMembershipUseCase);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, callerUserId, role, Instant.now());
    }

    private SpaceMembership destinationMembership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), destinationSpaceId, callerUserId, role, Instant.now());
    }

    private Task task(UUID inSpace, UUID recurringSeriesId, List<Subtask> subtasks, List<UUID> assigneeIds) {
        return new Task(taskId, inSpace, "Sortir les poubelles", TaskStatus.TODO, TaskPriority.MED,
            LocalDate.of(2026, 1, 7), assigneeIds, subtasks, recurringSeriesId, Instant.now());
    }

    @Test
    void a_member_can_move_a_task_which_clears_assignees_and_the_recurring_link_and_preserves_subtask_state() {
        UUID alice = UUID.randomUUID();
        List<Subtask> subtasks = List.of(new Subtask(UUID.randomUUID(), "Vérifier le tri", true),
            new Subtask(UUID.randomUUID(), "Sortir les bacs", false));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task(spaceId, seriesId, subtasks, List.of(alice))));
        when(resolveMembershipUseCase.resolve(destinationSpaceId, callerUserId)).thenReturn(destinationMembership(SpaceRole.MEMBER));
        Task created = task(destinationSpaceId, null, subtasks, List.of());
        when(taskRepository.create(new CreateTaskCommand(destinationSpaceId, "Sortir les poubelles", TaskPriority.MED,
            LocalDate.of(2026, 1, 7), List.of(), List.of(new SubtaskInput("Vérifier le tri", true),
                new SubtaskInput("Sortir les bacs", false)), null))).thenReturn(created);

        Task result = handler.move(taskId, destinationSpaceId, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(created);
        verify(taskRepository).delete(taskId);
    }

    @Test
    void a_viewer_cannot_move_a_task() {
        assertThatThrownBy(() -> handler.move(taskId, destinationSpaceId, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }

    @Test
    void a_task_from_another_space_is_not_found() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task(UUID.randomUUID(), null, List.of(), List.of())));

        assertThatThrownBy(() -> handler.move(taskId, destinationSpaceId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(TaskException.TaskNotFound.class);
        verify(taskRepository, never()).delete(taskId);
    }

    @Test
    void moving_into_the_same_space_is_rejected_before_touching_the_repository() {
        assertThatThrownBy(() -> handler.move(taskId, spaceId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(TaskException.SameSpaceTransfer.class);
        verify(taskRepository, never()).delete(taskId);
    }

    @Test
    void moving_into_a_space_the_caller_does_not_belong_to_is_not_found() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task(spaceId, null, List.of(), List.of())));
        when(resolveMembershipUseCase.resolve(destinationSpaceId, callerUserId)).thenThrow(new SpaceException.NotAMember());

        assertThatThrownBy(() -> handler.move(taskId, destinationSpaceId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(SpaceException.NotAMember.class);
        verify(taskRepository, never()).delete(taskId);
    }

    @Test
    void moving_into_a_space_where_the_caller_can_only_view_is_rejected() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task(spaceId, null, List.of(), List.of())));
        when(resolveMembershipUseCase.resolve(destinationSpaceId, callerUserId)).thenReturn(destinationMembership(SpaceRole.VIEWER));

        assertThatThrownBy(() -> handler.move(taskId, destinationSpaceId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
        verify(taskRepository, never()).delete(taskId);
    }
}
