package com.nido.api.tasks.application.handler;

import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.tasks.domain.model.CreateTaskCommand;
import com.nido.api.tasks.domain.model.RecurrenceInterval;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.model.RecurringTaskSeriesSchedule;
import com.nido.api.tasks.domain.model.TaskPriority;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterializeDueRecurringTasksHandlerTest {

    @Mock TaskRepository taskRepository;
    @Mock RecurringTaskSeriesRepository seriesRepository;
    @Mock GetSpaceTodayUseCase spaceToday;

    private MaterializeDueRecurringTasksHandler handler;
    private final UUID spaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new MaterializeDueRecurringTasksHandler(taskRepository, seriesRepository, spaceToday);
    }

    @Test
    void creates_the_occurrences_that_have_fallen_due_under_the_space_lock() {
        UUID seriesId = UUID.randomUUID();
        RecurringTaskSeries series = new RecurringTaskSeries(seriesId, spaceId, "Sortir les poubelles", TaskPriority.MED, List.of(),
            RecurrenceInterval.DAILY, 1, RecurrenceInterval.DAILY, 0, LocalDate.of(2026, 1, 7), null, 0, List.of(), 0, null);
        when(seriesRepository.findSchedulesBySpaceId(spaceId)).thenReturn(List.of(scheduleOf(series)));
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of(series));

        handler.materialize(membership(), LocalDate.of(2026, 1, 8));

        InOrder order = inOrder(seriesRepository, taskRepository);
        order.verify(seriesRepository).lockForMaterialization(spaceId);
        order.verify(taskRepository).createAll(List.of(new CreateTaskCommand(
            spaceId, "Sortir les poubelles", TaskPriority.MED, LocalDate.of(2026, 1, 8), List.of(), List.of(), seriesId, null)));
    }

    @Test
    void takes_no_lock_and_writes_nothing_when_nothing_is_due() {
        when(seriesRepository.findSchedulesBySpaceId(spaceId)).thenReturn(List.of());

        handler.materialize(membership(), LocalDate.of(2026, 1, 8));

        verify(seriesRepository, never()).lockForMaterialization(spaceId);
        verify(taskRepository, never()).createAll(any());
    }

    @Test
    void uses_the_space_own_today_when_called_without_one() {
        when(spaceToday.today(spaceId)).thenReturn(LocalDate.of(2026, 1, 8));
        when(seriesRepository.findSchedulesBySpaceId(spaceId)).thenReturn(List.of());

        handler.materialize(membership());

        verify(spaceToday).today(spaceId);
    }

    private SpaceMembership membership() {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.VIEWER, Instant.now());
    }

    /** The pre-check and the locked pass must see the same series, so the schedule is derived from it. */
    private static RecurringTaskSeriesSchedule scheduleOf(RecurringTaskSeries s) {
        return new RecurringTaskSeriesSchedule(s.id(), s.intervalType(), s.intervalCount(),
            s.leadIntervalType(), s.leadIntervalCount(), s.anchorDate(), s.endDate(), s.occurrenceCount());
    }
}
