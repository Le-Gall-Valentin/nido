package com.nido.api.tasks.application.handler;

import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.tasks.domain.model.CreateTaskCommand;
import com.nido.api.tasks.domain.model.RecurrenceInterval;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskPriority;
import com.nido.api.tasks.domain.model.TaskStatus;
import com.nido.api.tasks.domain.port.out.RecurringTaskSeriesRepository;
import com.nido.api.tasks.domain.port.out.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListTasksHandlerTest {

    @Mock TaskRepository taskRepository;
    @Mock RecurringTaskSeriesRepository seriesRepository;
    private ListTasksHandler handler;
    private final UUID spaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ListTasksHandler(taskRepository, seriesRepository);
    }

    private SpaceMembership membership() {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());
    }

    private Task task(TaskPriority priority) {
        return new Task(UUID.randomUUID(), spaceId, "T", TaskStatus.TODO, priority, null, List.of(), List.of(), null, Instant.now());
    }

    @Test
    void lists_the_callers_space_tasks_ordered_by_priority() {
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of());
        Task low = task(TaskPriority.LOW);
        Task high = task(TaskPriority.HIGH);
        when(taskRepository.findBySpaceId(spaceId)).thenReturn(List.of(low, high));

        List<Task> result = handler.list(membership());

        assertThat(result).containsExactly(high, low);
    }

    @Test
    void materializes_due_recurring_occurrences_before_listing() {
        LocalDate anchor = LocalDate.of(2026, 1, 7);
        UUID seriesId = UUID.randomUUID();
        RecurringTaskSeries series = new RecurringTaskSeries(seriesId, spaceId, "Sortir les poubelles", TaskPriority.MED, List.of(),
            RecurrenceInterval.DAILY, 1, RecurrenceInterval.DAILY, 0, anchor, null, 0, List.of(), 0);
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of(series));
        Task materialized = task(TaskPriority.MED);
        when(taskRepository.create(any())).thenReturn(materialized);
        when(taskRepository.findBySpaceId(spaceId)).thenReturn(List.of(materialized));

        List<Task> result = handler.list(membership(), LocalDate.of(2026, 1, 8));

        InOrder order = inOrder(seriesRepository, taskRepository);
        order.verify(seriesRepository).lockForMaterialization(spaceId);
        order.verify(taskRepository).create(new CreateTaskCommand(
            spaceId, "Sortir les poubelles", TaskPriority.MED, LocalDate.of(2026, 1, 8), List.of(), List.of(), seriesId));
        order.verify(taskRepository).findBySpaceId(spaceId);
        assertThat(result).containsExactly(materialized);
    }
}
