package com.nido.api.tasks.application.handler;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringTaskSeriesMaterializerTest {

    @Mock TaskRepository taskRepository;
    @Mock RecurringTaskSeriesRepository seriesRepository;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID seriesId = UUID.randomUUID();
    private final LocalDate anchor = LocalDate.of(2026, 1, 7);

    @BeforeEach
    void setUp() {
        lenient().when(taskRepository.create(any())).thenAnswer(invocation -> {
            CreateTaskCommand command = invocation.getArgument(0);
            return new Task(UUID.randomUUID(), command.spaceId(), command.title(), TaskStatus.TODO, command.priority(),
                command.dueDate(), command.assigneeIds(), List.of(), command.recurringSeriesId(), Instant.now());
        });
    }

    private RecurringTaskSeries series(RecurrenceInterval leadType, int leadCount, LocalDate endDate,
                                        int occurrenceCount, List<UUID> rotationMemberIds, int currentRotationIndex) {
        return new RecurringTaskSeries(seriesId, spaceId, "Sortir les poubelles", TaskPriority.MED, List.of(),
            RecurrenceInterval.WEEKLY, 1, leadType, leadCount, anchor, endDate,
            occurrenceCount, rotationMemberIds, currentRotationIndex);
    }

    @Test
    void nothing_is_materialized_when_the_next_occurrences_window_has_not_opened_yet() {
        RecurringTaskSeries s = series(RecurrenceInterval.DAILY, 0, null, 0, List.of(), 0);
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of(s));

        RecurringTaskSeriesMaterializer.materializeDueOccurrences(taskRepository, seriesRepository, spaceId, LocalDate.of(2026, 1, 13));

        verify(taskRepository, never()).create(any());
        verify(seriesRepository, never()).advance(any(), anyInt(), anyInt());
    }

    @Test
    void a_single_due_occurrence_is_materialized_and_the_series_advances() {
        RecurringTaskSeries s = series(RecurrenceInterval.DAILY, 0, null, 0, List.of(), 0);
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of(s));

        RecurringTaskSeriesMaterializer.materializeDueOccurrences(taskRepository, seriesRepository, spaceId, LocalDate.of(2026, 1, 14));

        verify(taskRepository).create(new CreateTaskCommand(spaceId, "Sortir les poubelles", TaskPriority.MED,
            LocalDate.of(2026, 1, 14), List.of(), List.of(), seriesId));
        verify(seriesRepository).advance(seriesId, 0, 1);
    }

    @Test
    void a_lead_time_opens_the_window_before_the_due_date() {
        RecurringTaskSeries s = series(RecurrenceInterval.DAILY, 3, null, 0, List.of(), 0);
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of(s));

        // Occurrence #1 is due 2026-01-14; with a 3-day lead its window opens 2026-01-11.
        RecurringTaskSeriesMaterializer.materializeDueOccurrences(taskRepository, seriesRepository, spaceId, LocalDate.of(2026, 1, 11));

        verify(taskRepository).create(new CreateTaskCommand(spaceId, "Sortir les poubelles", TaskPriority.MED,
            LocalDate.of(2026, 1, 14), List.of(), List.of(), seriesId));
        verify(seriesRepository).advance(seriesId, 0, 1);
    }

    @Test
    void several_overdue_occurrences_are_materialized_in_one_pass_with_rotation_advancing_each_time() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        RecurringTaskSeries s = series(RecurrenceInterval.DAILY, 0, null, 0, List.of(alice, bob), 0);
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of(s));

        // occurrence #1 due 2026-01-14, #2 due 2026-01-21, #3 due 2026-01-28 — all <= today.
        RecurringTaskSeriesMaterializer.materializeDueOccurrences(taskRepository, seriesRepository, spaceId, LocalDate.of(2026, 1, 28));

        verify(taskRepository).create(new CreateTaskCommand(spaceId, "Sortir les poubelles", TaskPriority.MED,
            LocalDate.of(2026, 1, 14), List.of(bob), List.of(), seriesId));
        verify(taskRepository).create(new CreateTaskCommand(spaceId, "Sortir les poubelles", TaskPriority.MED,
            LocalDate.of(2026, 1, 21), List.of(alice), List.of(), seriesId));
        verify(taskRepository).create(new CreateTaskCommand(spaceId, "Sortir les poubelles", TaskPriority.MED,
            LocalDate.of(2026, 1, 28), List.of(bob), List.of(), seriesId));
        verify(seriesRepository).advance(seriesId, 1, 3);
    }

    @Test
    void nothing_is_materialized_past_the_series_end_date() {
        RecurringTaskSeries s = series(RecurrenceInterval.DAILY, 0, LocalDate.of(2026, 1, 10), 0, List.of(), 0);
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of(s));

        RecurringTaskSeriesMaterializer.materializeDueOccurrences(taskRepository, seriesRepository, spaceId, LocalDate.of(2026, 2, 1));

        verify(taskRepository, never()).create(any());
        verify(seriesRepository, never()).advance(any(), anyInt(), anyInt());
    }

    @Test
    void subtask_templates_are_recopied_unchecked_onto_every_materialized_occurrence() {
        RecurringTaskSeries base = series(RecurrenceInterval.DAILY, 0, null, 0, List.of(), 0);
        RecurringTaskSeries withTemplates = new RecurringTaskSeries(base.id(), base.spaceId(), base.title(), base.priority(),
            List.of("Vérifier le tri"), base.intervalType(), base.intervalCount(), base.leadIntervalType(), base.leadIntervalCount(),
            base.anchorDate(), base.endDate(), base.occurrenceCount(), base.rotationMemberIds(), base.currentRotationIndex());
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of(withTemplates));

        RecurringTaskSeriesMaterializer.materializeDueOccurrences(taskRepository, seriesRepository, spaceId, LocalDate.of(2026, 1, 14));

        verify(taskRepository).create(new CreateTaskCommand(spaceId, "Sortir les poubelles", TaskPriority.MED,
            LocalDate.of(2026, 1, 14), List.of(), List.of(new SubtaskInput("Vérifier le tri", false)), seriesId));
    }

    @Test
    void locks_the_space_for_materialization_before_reading_any_series() {
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of());

        RecurringTaskSeriesMaterializer.materializeDueOccurrences(taskRepository, seriesRepository, spaceId, LocalDate.of(2026, 1, 14));

        verify(seriesRepository, times(1)).lockForMaterialization(spaceId);
    }

    private static int anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }
}
