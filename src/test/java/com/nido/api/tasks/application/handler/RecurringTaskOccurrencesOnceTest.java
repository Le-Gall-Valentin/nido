package com.nido.api.tasks.application.handler;

import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.tasks.domain.model.CreateTaskCommand;
import com.nido.api.tasks.domain.model.ProjectedTaskOccurrence;
import com.nido.api.tasks.domain.model.RecurrenceInterval;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.model.RecurringTaskSeriesSchedule;
import com.nido.api.tasks.domain.model.TaskPriority;
import com.nido.api.tasks.domain.port.out.RecurringTaskSeriesRepository;
import com.nido.api.tasks.domain.port.out.TaskRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The calendar shows a recurring task's past and present from the real tasks, and its future from
 * the projection. The two are written by different classes — the materializer advances the series'
 * count, the projection reads it — and each once assumed a different meaning for that count. This
 * runs the real pair end to end: every date of the series must come out exactly once.
 */
class RecurringTaskOccurrencesOnceTest {

    private final UUID spaceId = UUID.randomUUID();
    private final UUID seriesId = UUID.randomUUID();
    private final LocalDate anchor = LocalDate.of(2026, 1, 7);

    @Test
    void everyDateOfASeriesIsEitherARealTaskOrProjectedNeverBoth() {
        // Created on Jan 7: creating a series writes the Jan 7 task itself, with a count of 0.
        RecurringTaskSeries created = weekly(0);
        List<LocalDate> realTasks = new ArrayList<>(List.of(anchor));

        // Two weeks later, a task read materializes what came due meanwhile.
        TaskRepository tasks = mock(TaskRepository.class);
        RecurringTaskSeriesRepository seriesRepository = mock(RecurringTaskSeriesRepository.class);
        when(seriesRepository.findSchedulesBySpaceId(spaceId)).thenReturn(List.of(scheduleOf(created)));
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of(created));
        RecurringTaskSeriesMaterializer.materializeDueOccurrences(tasks, seriesRepository, spaceId, LocalDate.of(2026, 1, 21));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CreateTaskCommand>> written = ArgumentCaptor.forClass(List.class);
        verify(tasks).createAll(written.capture());
        written.getValue().forEach(command -> realTasks.add(command.dueDate()));
        ArgumentCaptor<Integer> count = ArgumentCaptor.forClass(Integer.class);
        verify(seriesRepository).advance(eq(seriesId), anyInt(), count.capture());

        // The calendar then projects the rest of the window from the advanced series.
        RecurringTaskSeriesRepository advanced = mock(RecurringTaskSeriesRepository.class);
        when(advanced.findBySpaceId(spaceId)).thenReturn(List.of(weekly(count.getValue())));
        SpaceMembership caller = new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());
        List<LocalDate> projected = new ProjectRecurringTasksHandler(advanced)
            .project(caller, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 28)).stream()
            .map(ProjectedTaskOccurrence::dueDate).toList();

        List<LocalDate> shown = new ArrayList<>(realTasks);
        shown.addAll(projected);
        assertThat(shown).doesNotHaveDuplicates();
        assertThat(shown).containsExactlyInAnyOrder(
            LocalDate.of(2026, 1, 7), LocalDate.of(2026, 1, 14), LocalDate.of(2026, 1, 21), LocalDate.of(2026, 1, 28),
            LocalDate.of(2026, 2, 4), LocalDate.of(2026, 2, 11), LocalDate.of(2026, 2, 18), LocalDate.of(2026, 2, 25));
    }

    private RecurringTaskSeries weekly(int occurrenceCount) {
        return new RecurringTaskSeries(seriesId, spaceId, "Sortir les poubelles", TaskPriority.MED, List.of(),
            RecurrenceInterval.WEEKLY, 1, RecurrenceInterval.DAILY, 0, anchor, null,
            occurrenceCount, List.of(), 0, UUID.randomUUID());
    }

    private static RecurringTaskSeriesSchedule scheduleOf(RecurringTaskSeries s) {
        return new RecurringTaskSeriesSchedule(s.id(), s.intervalType(), s.intervalCount(),
            s.leadIntervalType(), s.leadIntervalCount(), s.anchorDate(), s.endDate(), s.occurrenceCount());
    }
}
