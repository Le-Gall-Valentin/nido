package com.nido.api.tasks.application.handler;

import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.tasks.domain.model.CreateTaskCommand;
import com.nido.api.tasks.domain.model.RecurrenceInterval;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.model.Subtask;
import com.nido.api.tasks.domain.model.SubtaskInput;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskException;
import com.nido.api.tasks.domain.model.TaskPriority;
import com.nido.api.tasks.domain.model.TaskStatus;
import com.nido.api.tasks.domain.port.out.RecurringTaskSeriesRepository;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeTaskStatusHandlerTest {

    @Mock TaskRepository taskRepository;
    @Mock RecurringTaskSeriesRepository seriesRepository;
    private ChangeTaskStatusHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID taskId = UUID.randomUUID();
    private final UUID seriesId = UUID.randomUUID();
    private final LocalDate anchor = LocalDate.of(2026, 1, 7);

    @BeforeEach
    void setUp() {
        handler = new ChangeTaskStatusHandler(taskRepository, seriesRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    private Task oneOffTask(TaskStatus status, List<Subtask> subtasks) {
        return new Task(taskId, spaceId, "T", status, TaskPriority.MED, null, List.of(), subtasks, null, Instant.now());
    }

    private Task recurringTask(TaskStatus status, List<Subtask> subtasks) {
        return new Task(taskId, spaceId, "T", status, TaskPriority.MED, anchor, List.of(), subtasks, seriesId, Instant.now());
    }

    @Test
    void a_member_can_move_a_task_between_non_terminal_statuses() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(oneOffTask(TaskStatus.TODO, List.of())));
        Task updated = oneOffTask(TaskStatus.DOING, List.of());
        when(taskRepository.updateStatus(taskId, TaskStatus.DOING)).thenReturn(updated);

        Task result = handler.changeStatus(taskId, spaceId, TaskStatus.DOING, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(updated);
        verifyNoInteractions(seriesRepository);
    }

    @Test
    void a_viewer_cannot_change_status() {
        assertThatThrownBy(() -> handler.changeStatus(taskId, spaceId, TaskStatus.DOING, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }

    @Test
    void a_task_from_another_space_is_not_found() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(
            new Task(taskId, UUID.randomUUID(), "T", TaskStatus.TODO, TaskPriority.MED, null, List.of(), List.of(), null, Instant.now())));

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
        verifyNoInteractions(seriesRepository);
    }

    @Test
    void completing_a_one_off_task_does_not_touch_the_series_repository() {
        Subtask done = new Subtask(UUID.randomUUID(), "Vérifier", true);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(oneOffTask(TaskStatus.DOING, List.of(done))));
        when(taskRepository.updateStatus(taskId, TaskStatus.DONE)).thenReturn(oneOffTask(TaskStatus.DONE, List.of(done)));

        handler.changeStatus(taskId, spaceId, TaskStatus.DONE, membership(SpaceRole.MEMBER));

        verifyNoInteractions(seriesRepository);
    }

    @Test
    void completing_a_recurring_task_generates_the_next_occurrence_with_the_anchored_due_date_and_no_rotation() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(recurringTask(TaskStatus.DOING, List.of())));
        when(taskRepository.updateStatus(taskId, TaskStatus.DONE)).thenReturn(recurringTask(TaskStatus.DONE, List.of()));
        RecurringTaskSeries series = new RecurringTaskSeries(seriesId, spaceId, "T", TaskPriority.MED,
            List.of(), RecurrenceInterval.WEEKLY, 1, anchor, 0, List.of(), 0);
        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(series));

        handler.changeStatus(taskId, spaceId, TaskStatus.DONE, membership(SpaceRole.MEMBER));

        verify(taskRepository).create(new CreateTaskCommand(
            spaceId, "T", TaskPriority.MED, LocalDate.of(2026, 1, 14), List.of(), List.of(), seriesId));
        verify(seriesRepository).advance(seriesId, 0, 1);
    }

    @Test
    void completing_a_recurring_task_with_rotation_assigns_the_next_member_and_advances_the_index() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(recurringTask(TaskStatus.DOING, List.of())));
        when(taskRepository.updateStatus(taskId, TaskStatus.DONE)).thenReturn(recurringTask(TaskStatus.DONE, List.of()));
        RecurringTaskSeries series = new RecurringTaskSeries(seriesId, spaceId, "T", TaskPriority.MED,
            List.of(), RecurrenceInterval.WEEKLY, 1, anchor, 0, List.of(alice, bob), 0);
        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(series));

        handler.changeStatus(taskId, spaceId, TaskStatus.DONE, membership(SpaceRole.MEMBER));

        verify(taskRepository).create(new CreateTaskCommand(
            spaceId, "T", TaskPriority.MED, LocalDate.of(2026, 1, 14), List.of(bob), List.of(), seriesId));
        verify(seriesRepository).advance(seriesId, 1, 1);
    }

    @Test
    void the_rotation_wraps_around_to_the_first_member() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(recurringTask(TaskStatus.DOING, List.of())));
        when(taskRepository.updateStatus(taskId, TaskStatus.DONE)).thenReturn(recurringTask(TaskStatus.DONE, List.of()));
        // currentRotationIndex = 1 (bob is currently assigned, the last in the list) — the next
        // occurrence must wrap back around to alice at index 0.
        RecurringTaskSeries series = new RecurringTaskSeries(seriesId, spaceId, "T", TaskPriority.MED,
            List.of(), RecurrenceInterval.WEEKLY, 1, anchor, 3, List.of(alice, bob), 1);
        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(series));

        handler.changeStatus(taskId, spaceId, TaskStatus.DONE, membership(SpaceRole.MEMBER));

        // anchor (2026-01-07) + 1 week * occurrence #4 = + 28 days = 2026-02-04.
        verify(taskRepository).create(new CreateTaskCommand(
            spaceId, "T", TaskPriority.MED, LocalDate.of(2026, 2, 4), List.of(alice), List.of(), seriesId));
        verify(seriesRepository).advance(seriesId, 0, 4);
    }

    @Test
    void a_recurring_task_with_subtask_templates_recopies_them_all_unchecked_on_the_next_occurrence() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(recurringTask(TaskStatus.DOING, List.of())));
        when(taskRepository.updateStatus(taskId, TaskStatus.DONE)).thenReturn(recurringTask(TaskStatus.DONE, List.of()));
        RecurringTaskSeries series = new RecurringTaskSeries(seriesId, spaceId, "T", TaskPriority.MED,
            List.of("Vérifier le tri", "Sortir les bacs"), RecurrenceInterval.WEEKLY, 1, anchor, 0, List.of(), 0);
        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(series));

        handler.changeStatus(taskId, spaceId, TaskStatus.DONE, membership(SpaceRole.MEMBER));

        verify(taskRepository).create(new CreateTaskCommand(
            spaceId, "T", TaskPriority.MED, LocalDate.of(2026, 1, 14), List.of(),
            List.of(new SubtaskInput("Vérifier le tri", false), new SubtaskInput("Sortir les bacs", false)), seriesId));
    }
}
