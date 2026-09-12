package com.nido.api.tasks.application.handler;

import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.tasks.application.service.TaskSpaceMemberValidator;
import com.nido.api.tasks.domain.model.CreateRecurringTaskSeriesCommand;
import com.nido.api.tasks.domain.model.CreateTaskCommand;
import com.nido.api.tasks.domain.model.RecurrenceInterval;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateRecurringTaskHandlerTest {

    @Mock TaskRepository taskRepository;

    @Mock GetSpaceTodayUseCase spaceToday;
    @Mock RecurringTaskSeriesRepository seriesRepository;
    @Mock TaskSpaceMemberValidator spaceMemberValidator;
    private CreateRecurringTaskHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID seriesId = UUID.randomUUID();
    private final UUID creatorId = UUID.randomUUID();
    private final LocalDate anchor = LocalDate.of(2026, 1, 7);

    @BeforeEach
    void setUp() {
        // The space's date, not the server's: these handlers validate a backlog against
        // "today" and the space is what decides which day that is.
        lenient().when(spaceToday.today(any())).thenReturn(LocalDate.of(2026, 9, 12));
        handler = new CreateRecurringTaskHandler(taskRepository, seriesRepository, spaceMemberValidator, spaceToday);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    private CreateRecurringTaskSeriesCommand command(int leadIntervalCount, LocalDate endDate, List<UUID> rotationMemberIds) {
        return new CreateRecurringTaskSeriesCommand(spaceId, "Sortir les poubelles", TaskPriority.MED, List.of("Vérifier le tri"),
            RecurrenceInterval.WEEKLY, 1, RecurrenceInterval.DAILY, leadIntervalCount, anchor, endDate, rotationMemberIds, creatorId);
    }

    private RecurringTaskSeries series(List<UUID> rotationMemberIds) {
        return new RecurringTaskSeries(seriesId, spaceId, "Sortir les poubelles", TaskPriority.MED,
            List.of("Vérifier le tri"), RecurrenceInterval.WEEKLY, 1, RecurrenceInterval.DAILY, 2, anchor, null, 0, rotationMemberIds, 0,
            creatorId);
    }

    @Test
    void creates_the_series_and_its_first_occurrence_assigned_to_the_first_rotation_member() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        CreateRecurringTaskSeriesCommand command = command(2, null, List.of(alice, bob));
        when(seriesRepository.create(command)).thenReturn(series(List.of(alice, bob)));
        Task firstOccurrence = new Task(UUID.randomUUID(), spaceId, "Sortir les poubelles", TaskStatus.TODO,
            TaskPriority.MED, anchor, List.of(alice), List.of(), seriesId, creatorId, Instant.now());
        when(taskRepository.create(new CreateTaskCommand(spaceId, "Sortir les poubelles", TaskPriority.MED, anchor,
            List.of(alice), List.of(new SubtaskInput("Vérifier le tri", false)), seriesId, creatorId))).thenReturn(firstOccurrence);

        Task result = handler.create(command, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(firstOccurrence);
    }

    @Test
    void a_series_with_no_rotation_members_creates_its_first_occurrence_unassigned() {
        CreateRecurringTaskSeriesCommand command = command(2, null, List.of());
        when(seriesRepository.create(command)).thenReturn(series(List.of()));
        Task firstOccurrence = new Task(UUID.randomUUID(), spaceId, "Sortir les poubelles", TaskStatus.TODO,
            TaskPriority.MED, anchor, List.of(), List.of(), seriesId, creatorId, Instant.now());
        when(taskRepository.create(new CreateTaskCommand(spaceId, "Sortir les poubelles", TaskPriority.MED, anchor,
            List.of(), List.of(new SubtaskInput("Vérifier le tri", false)), seriesId, creatorId))).thenReturn(firstOccurrence);

        Task result = handler.create(command, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(firstOccurrence);
    }

    @Test
    void a_viewer_cannot_create_a_recurring_task() {
        CreateRecurringTaskSeriesCommand command = command(2, null, List.of());

        assertThatThrownBy(() -> handler.create(command, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }

    @Test
    void a_lead_time_longer_than_the_recurrence_interval_is_rejected() {
        // Interval is WEEKLY/1 (7 days out); a DAILY/8 lead would open the window
        // before the series even starts, which is longer than the interval itself.
        CreateRecurringTaskSeriesCommand command = command(8, null, List.of());

        assertThatThrownBy(() -> handler.create(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(TaskException.LeadTimeExceedsInterval.class);
        verify(seriesRepository, never()).create(any());
    }

    @Test
    void an_end_date_before_the_anchor_date_is_rejected() {
        CreateRecurringTaskSeriesCommand command = command(2, anchor.minusDays(1), List.of());

        assertThatThrownBy(() -> handler.create(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(TaskException.InvalidEndDate.class);
        verify(seriesRepository, never()).create(any());
    }

    @Test
    void a_rotation_member_who_is_not_in_the_space_is_rejected() {
        UUID stranger = UUID.randomUUID();
        CreateRecurringTaskSeriesCommand command = command(0, null, List.of(stranger));
        doThrow(new TaskException.MemberNotInSpace()).when(spaceMemberValidator).ensureMember(spaceId, stranger);

        assertThatThrownBy(() -> handler.create(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(TaskException.MemberNotInSpace.class);
        verify(seriesRepository, never()).create(any());
    }
}
