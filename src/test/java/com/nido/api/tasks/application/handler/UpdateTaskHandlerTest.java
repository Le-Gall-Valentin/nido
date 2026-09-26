package com.nido.api.tasks.application.handler;

import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.tasks.application.service.TaskSpaceMemberValidator;
import com.nido.api.tasks.domain.model.Subtask;
import com.nido.api.tasks.domain.model.SubtaskEdit;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskException;
import com.nido.api.tasks.domain.model.TaskPriority;
import com.nido.api.tasks.domain.model.TaskStatus;
import com.nido.api.tasks.domain.model.UpdateTaskCommand;
import com.nido.api.tasks.domain.port.out.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateTaskHandlerTest {

    @Mock TaskRepository taskRepository;
    @Mock TaskSpaceMemberValidator spaceMemberValidator;
    private UpdateTaskHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID taskId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new UpdateTaskHandler(taskRepository, spaceMemberValidator);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    private Task task(UUID inSpace) {
        return new Task(taskId, inSpace, "Ancien titre", TaskStatus.TODO, TaskPriority.LOW, null, List.of(), List.of(), null, null, Instant.now());
    }

    @Test
    void a_member_can_update_a_task_in_their_space() {
        UpdateTaskCommand command = new UpdateTaskCommand(taskId, spaceId, "Nouveau titre", TaskPriority.HIGH, null, List.of(), null);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task(spaceId)));
        Task updated = new Task(taskId, spaceId, "Nouveau titre", TaskStatus.TODO, TaskPriority.HIGH, null, List.of(), List.of(), null, null, Instant.now());
        when(taskRepository.update(command)).thenReturn(updated);

        Task result = handler.update(command, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(updated);
    }

    @Test
    void a_task_from_another_space_is_not_found() {
        UpdateTaskCommand command = new UpdateTaskCommand(taskId, spaceId, "Nouveau titre", TaskPriority.HIGH, null, List.of(), null);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task(UUID.randomUUID())));

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(TaskException.TaskNotFound.class);
    }

    @Test
    void a_viewer_cannot_update_a_task() {
        UpdateTaskCommand command = new UpdateTaskCommand(taskId, spaceId, "Nouveau titre", TaskPriority.HIGH, null, List.of(), null);

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.VIEWER)))
            .isInstanceOf(com.nido.api.space.domain.model.SpaceException.InsufficientRole.class);
    }

    @Test
    void an_assignee_who_is_not_in_the_space_is_rejected() {
        UUID stranger = UUID.randomUUID();
        UpdateTaskCommand command = new UpdateTaskCommand(taskId, spaceId, "Nouveau titre", TaskPriority.HIGH, null, List.of(stranger), null);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task(spaceId)));
        doThrow(new TaskException.MemberNotInSpace()).when(spaceMemberValidator).ensureMember(spaceId, stranger);

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(TaskException.MemberNotInSpace.class);
        verify(taskRepository, never()).update(any());
    }

    // ─── Les sous-tâches et ce qu'elles font au statut ─────────────────────

    private final UUID checkedId = UUID.randomUUID();

    private Task taskWith(TaskStatus status, LocalDate dueDate, List<Subtask> subtasks) {
        return new Task(taskId, spaceId, "Ancien titre", status, TaskPriority.LOW, dueDate, List.of(), subtasks, null, null, Instant.now());
    }

    private UpdateTaskCommand editing(LocalDate dueDate, List<SubtaskEdit> subtasks) {
        return new UpdateTaskCommand(taskId, spaceId, "Titre", TaskPriority.LOW, dueDate, List.of(), subtasks);
    }

    @Test
    void a_subtask_of_another_task_is_not_found() {
        // The adapter looks subtasks up by their own id: a foreign one would be renamed and moved
        // into this task's list, wherever it lives — same hole ToggleSubtaskHandler closes.
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(
            taskWith(TaskStatus.TODO, null, List.of(new Subtask(checkedId, "A", true)))));

        assertThatThrownBy(() -> handler.update(
                editing(null, List.of(new SubtaskEdit(UUID.randomUUID(), "Ailleurs"))), membership(SpaceRole.MEMBER)))
            .isInstanceOf(TaskException.TaskNotFound.class);
        verify(taskRepository, never()).update(any());
    }

    @Test
    void the_same_subtask_listed_twice_is_rejected() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(
            taskWith(TaskStatus.TODO, null, List.of(new Subtask(checkedId, "A", true)))));

        assertThatThrownBy(() -> handler.update(
                editing(null, List.of(new SubtaskEdit(checkedId, "A"), new SubtaskEdit(checkedId, "A bis"))),
                membership(SpaceRole.MEMBER)))
            .isInstanceOf(TaskException.TaskNotFound.class);
        verify(taskRepository, never()).update(any());
    }

    @Test
    void a_done_task_given_an_unchecked_subtask_goes_back_to_doing() {
        Task done = taskWith(TaskStatus.DONE, null, List.of(new Subtask(checkedId, "A", true)));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(done));
        when(taskRepository.update(any())).thenReturn(done);
        Task reopened = taskWith(TaskStatus.DOING, null, List.of());
        when(taskRepository.updateStatus(taskId, TaskStatus.DOING)).thenReturn(reopened);

        Task result = handler.update(
            editing(null, List.of(new SubtaskEdit(checkedId, "A"), new SubtaskEdit(null, "Nouvelle"))),
            membership(SpaceRole.MEMBER));

        verify(taskRepository).updateStatus(taskId, TaskStatus.DOING);
        assertThat(result).as("the task as it ends up, not as it was before reopening").isEqualTo(reopened);
    }

    @Test
    void a_done_task_whose_subtasks_all_stay_checked_stays_done() {
        Task done = taskWith(TaskStatus.DONE, null, List.of(new Subtask(checkedId, "A", true)));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(done));
        when(taskRepository.update(any())).thenReturn(done);

        handler.update(editing(null, List.of(new SubtaskEdit(checkedId, "A renommée"))), membership(SpaceRole.MEMBER));

        verify(taskRepository, never()).updateStatus(any(), any());
    }

    @Test
    void an_open_task_given_a_new_subtask_keeps_its_status() {
        Task todo = taskWith(TaskStatus.TODO, null, List.of());
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(todo));
        when(taskRepository.update(any())).thenReturn(todo);

        handler.update(editing(null, List.of(new SubtaskEdit(null, "Nouvelle"))), membership(SpaceRole.MEMBER));

        verify(taskRepository, never()).updateStatus(any(), any());
    }

    // ─── L'échéance qu'une tâche terminée ne montre pas ─────────────────────

    private LocalDate savedDueDate(Task existing, LocalDate sent) {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existing));
        when(taskRepository.update(any())).thenReturn(existing);
        handler.update(editing(sent, null), membership(SpaceRole.MEMBER));
        ArgumentCaptor<UpdateTaskCommand> saved = ArgumentCaptor.forClass(UpdateTaskCommand.class);
        verify(taskRepository).update(saved.capture());
        return saved.getValue().dueDate();
    }

    @Test
    void a_done_task_keeps_the_due_date_it_does_not_show_when_none_is_sent() {
        // TaskResponse hides a completed task's due date, so the form comes back without one:
        // taking that at its word would erase a date the user never saw.
        LocalDate hidden = LocalDate.of(2026, 1, 7);

        assertThat(savedDueDate(taskWith(TaskStatus.DONE, hidden, List.of()), null)).isEqualTo(hidden);
    }

    @Test
    void a_done_task_given_a_due_date_takes_it() {
        LocalDate sent = LocalDate.of(2026, 2, 1);

        assertThat(savedDueDate(taskWith(TaskStatus.DONE, LocalDate.of(2026, 1, 7), List.of()), sent)).isEqualTo(sent);
    }

    @Test
    void an_open_task_can_have_its_due_date_cleared() {
        assertThat(savedDueDate(taskWith(TaskStatus.TODO, LocalDate.of(2026, 1, 7), List.of()), null)).isNull();
    }
}
