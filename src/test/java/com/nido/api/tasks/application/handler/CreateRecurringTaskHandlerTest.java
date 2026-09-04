package com.nido.api.tasks.application.handler;

import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.tasks.domain.model.CreateRecurringTaskSeriesCommand;
import com.nido.api.tasks.domain.model.CreateTaskCommand;
import com.nido.api.tasks.domain.model.RecurrenceInterval;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.model.SubtaskInput;
import com.nido.api.tasks.domain.model.Task;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateRecurringTaskHandlerTest {

    @Mock TaskRepository taskRepository;
    @Mock RecurringTaskSeriesRepository seriesRepository;
    private CreateRecurringTaskHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID seriesId = UUID.randomUUID();
    private final LocalDate anchor = LocalDate.of(2026, 1, 7);

    @BeforeEach
    void setUp() {
        handler = new CreateRecurringTaskHandler(taskRepository, seriesRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    private RecurringTaskSeries series(List<UUID> rotationMemberIds) {
        return new RecurringTaskSeries(seriesId, spaceId, "Sortir les poubelles", TaskPriority.MED,
            List.of("Vérifier le tri"), RecurrenceInterval.WEEKLY, 1, anchor, 0, rotationMemberIds, 0);
    }

    @Test
    void creates_the_series_and_its_first_occurrence_assigned_to_the_first_rotation_member() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        CreateRecurringTaskSeriesCommand command = new CreateRecurringTaskSeriesCommand(
            spaceId, "Sortir les poubelles", TaskPriority.MED, List.of("Vérifier le tri"),
            RecurrenceInterval.WEEKLY, 1, anchor, List.of(alice, bob));
        when(seriesRepository.create(command)).thenReturn(series(List.of(alice, bob)));
        Task firstOccurrence = new Task(UUID.randomUUID(), spaceId, "Sortir les poubelles", TaskStatus.TODO,
            TaskPriority.MED, anchor, List.of(alice), List.of(), seriesId, Instant.now());
        when(taskRepository.create(new CreateTaskCommand(spaceId, "Sortir les poubelles", TaskPriority.MED, anchor,
            List.of(alice), List.of(new SubtaskInput("Vérifier le tri", false)), seriesId))).thenReturn(firstOccurrence);

        Task result = handler.create(command, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(firstOccurrence);
    }

    @Test
    void a_series_with_no_rotation_members_creates_its_first_occurrence_unassigned() {
        CreateRecurringTaskSeriesCommand command = new CreateRecurringTaskSeriesCommand(
            spaceId, "Sortir les poubelles", TaskPriority.MED, List.of("Vérifier le tri"),
            RecurrenceInterval.WEEKLY, 1, anchor, List.of());
        when(seriesRepository.create(command)).thenReturn(series(List.of()));
        Task firstOccurrence = new Task(UUID.randomUUID(), spaceId, "Sortir les poubelles", TaskStatus.TODO,
            TaskPriority.MED, anchor, List.of(), List.of(), seriesId, Instant.now());
        when(taskRepository.create(new CreateTaskCommand(spaceId, "Sortir les poubelles", TaskPriority.MED, anchor,
            List.of(), List.of(new SubtaskInput("Vérifier le tri", false)), seriesId))).thenReturn(firstOccurrence);

        Task result = handler.create(command, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(firstOccurrence);
    }

    @Test
    void a_viewer_cannot_create_a_recurring_task() {
        CreateRecurringTaskSeriesCommand command = new CreateRecurringTaskSeriesCommand(
            spaceId, "Sortir les poubelles", TaskPriority.MED, List.of(), RecurrenceInterval.WEEKLY, 1, anchor, List.of());

        assertThatThrownBy(() -> handler.create(command, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }
}
