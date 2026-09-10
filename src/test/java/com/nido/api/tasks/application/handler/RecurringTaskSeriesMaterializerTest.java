package com.nido.api.tasks.application.handler;

import com.nido.api.tasks.domain.model.CreateTaskCommand;
import com.nido.api.tasks.domain.model.RecurrenceInterval;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.model.RecurringTaskSeriesSchedule;
import com.nido.api.tasks.domain.model.SubtaskInput;
import com.nido.api.tasks.domain.model.TaskPriority;
import com.nido.api.tasks.domain.port.out.RecurringTaskSeriesRepository;
import com.nido.api.tasks.domain.port.out.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
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
    private final UUID creatorId = UUID.randomUUID();
    private final LocalDate anchor = LocalDate.of(2026, 1, 7);

    private RecurringTaskSeries series(RecurrenceInterval leadType, int leadCount, LocalDate endDate,
                                        int occurrenceCount, List<UUID> rotationMemberIds, int currentRotationIndex) {
        return new RecurringTaskSeries(seriesId, spaceId, "Sortir les poubelles", TaskPriority.MED, List.of(),
            RecurrenceInterval.WEEKLY, 1, leadType, leadCount, anchor, endDate,
            occurrenceCount, rotationMemberIds, currentRotationIndex, creatorId);
    }

    @Test
    void nothing_is_materialized_when_the_next_occurrences_window_has_not_opened_yet() {
        RecurringTaskSeries s = series(RecurrenceInterval.DAILY, 0, null, 0, List.of(), 0);
        when(seriesRepository.findSchedulesBySpaceId(spaceId))
            .thenReturn(List.of(scheduleOf(s)));

        RecurringTaskSeriesMaterializer.materializeDueOccurrences(taskRepository, seriesRepository, spaceId, LocalDate.of(2026, 1, 13));

        verify(taskRepository, never()).createAll(any());
        verify(seriesRepository, never()).advance(any(), anyInt(), anyInt());
    }

    @Test
    void a_single_due_occurrence_is_materialized_and_the_series_advances() {
        RecurringTaskSeries s = series(RecurrenceInterval.DAILY, 0, null, 0, List.of(), 0);
        when(seriesRepository.findSchedulesBySpaceId(spaceId))
            .thenReturn(List.of(scheduleOf(s)));
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of(s));

        RecurringTaskSeriesMaterializer.materializeDueOccurrences(taskRepository, seriesRepository, spaceId, LocalDate.of(2026, 1, 14));

        verify(taskRepository).createAll(List.of(new CreateTaskCommand(spaceId, "Sortir les poubelles", TaskPriority.MED,
            LocalDate.of(2026, 1, 14), List.of(), List.of(), seriesId, creatorId)));
        verify(seriesRepository).advance(seriesId, 0, 1);
    }

    @Test
    void a_lead_time_opens_the_window_before_the_due_date() {
        RecurringTaskSeries s = series(RecurrenceInterval.DAILY, 3, null, 0, List.of(), 0);
        when(seriesRepository.findSchedulesBySpaceId(spaceId))
            .thenReturn(List.of(scheduleOf(s)));
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of(s));

        // Occurrence #1 is due 2026-01-14; with a 3-day lead its window opens 2026-01-11.
        RecurringTaskSeriesMaterializer.materializeDueOccurrences(taskRepository, seriesRepository, spaceId, LocalDate.of(2026, 1, 11));

        verify(taskRepository).createAll(List.of(new CreateTaskCommand(spaceId, "Sortir les poubelles", TaskPriority.MED,
            LocalDate.of(2026, 1, 14), List.of(), List.of(), seriesId, creatorId)));
        verify(seriesRepository).advance(seriesId, 0, 1);
    }

    @Test
    void several_overdue_occurrences_are_materialized_in_a_single_batch_with_rotation_advancing_each_time() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        RecurringTaskSeries s = series(RecurrenceInterval.DAILY, 0, null, 0, List.of(alice, bob), 0);
        when(seriesRepository.findSchedulesBySpaceId(spaceId))
            .thenReturn(List.of(scheduleOf(s)));
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of(s));

        // occurrence #1 due 2026-01-14, #2 due 2026-01-21, #3 due 2026-01-28 — all <= today.
        RecurringTaskSeriesMaterializer.materializeDueOccurrences(taskRepository, seriesRepository, spaceId, LocalDate.of(2026, 1, 28));

        // A single batched call for all three overdue occurrences — not one create() per
        // occurrence — so catching up after a long absence costs one write, not N.
        verify(taskRepository).createAll(List.of(
            new CreateTaskCommand(spaceId, "Sortir les poubelles", TaskPriority.MED,
                LocalDate.of(2026, 1, 14), List.of(bob), List.of(), seriesId, creatorId),
            new CreateTaskCommand(spaceId, "Sortir les poubelles", TaskPriority.MED,
                LocalDate.of(2026, 1, 21), List.of(alice), List.of(), seriesId, creatorId),
            new CreateTaskCommand(spaceId, "Sortir les poubelles", TaskPriority.MED,
                LocalDate.of(2026, 1, 28), List.of(bob), List.of(), seriesId, creatorId)));
        verify(seriesRepository).advance(seriesId, 1, 3);
    }

    @Test
    void nothing_is_materialized_past_the_series_end_date() {
        RecurringTaskSeries s = series(RecurrenceInterval.DAILY, 0, LocalDate.of(2026, 1, 10), 0, List.of(), 0);
        when(seriesRepository.findSchedulesBySpaceId(spaceId))
            .thenReturn(List.of(scheduleOf(s)));

        RecurringTaskSeriesMaterializer.materializeDueOccurrences(taskRepository, seriesRepository, spaceId, LocalDate.of(2026, 2, 1));

        verify(taskRepository, never()).createAll(any());
        verify(seriesRepository, never()).advance(any(), anyInt(), anyInt());
    }

    @Test
    void subtask_templates_are_recopied_unchecked_onto_every_materialized_occurrence() {
        RecurringTaskSeries base = series(RecurrenceInterval.DAILY, 0, null, 0, List.of(), 0);
        RecurringTaskSeries withTemplates = new RecurringTaskSeries(base.id(), base.spaceId(), base.title(), base.priority(),
            List.of("Vérifier le tri"), base.intervalType(), base.intervalCount(), base.leadIntervalType(), base.leadIntervalCount(),
            base.anchorDate(), base.endDate(), base.occurrenceCount(), base.rotationMemberIds(), base.currentRotationIndex(),
            base.createdBy());
        when(seriesRepository.findSchedulesBySpaceId(spaceId))
            .thenReturn(List.of(scheduleOf(withTemplates)));
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of(withTemplates));

        RecurringTaskSeriesMaterializer.materializeDueOccurrences(taskRepository, seriesRepository, spaceId, LocalDate.of(2026, 1, 14));

        verify(taskRepository).createAll(List.of(new CreateTaskCommand(spaceId, "Sortir les poubelles", TaskPriority.MED,
            LocalDate.of(2026, 1, 14), List.of(), List.of(new SubtaskInput("Vérifier le tri", false)), seriesId, creatorId)));
    }

    @Test
    void takes_no_lock_at_all_when_no_series_owes_an_occurrence() {
        // The ordinary case, and the reason the schedule pre-check exists: a read that has
        // nothing to materialize must not take the space's advisory lock, which would serialize
        // every other reader behind it, nor load a single series.
        when(seriesRepository.findSchedulesBySpaceId(spaceId)).thenReturn(List.of());

        RecurringTaskSeriesMaterializer.materializeDueOccurrences(taskRepository, seriesRepository, spaceId, LocalDate.of(2026, 1, 14));

        verify(seriesRepository, never()).lockForMaterialization(spaceId);
        verify(seriesRepository, never()).findBySpaceId(spaceId);
        verify(taskRepository, never()).createAll(any());
    }

    @Test
    void locks_the_space_before_loading_the_series_it_is_about_to_materialize() {
        // The pre-check reads before the lock, but everything the writes are derived from is
        // read after it: an entity loaded beforehand would be handed back stale by the read
        // under the lock, and a concurrent materialization would be invisible.
        RecurringTaskSeries s = series(RecurrenceInterval.DAILY, 0, null, 0, List.of(), 0);
        when(seriesRepository.findSchedulesBySpaceId(spaceId)).thenReturn(List.of(scheduleOf(s)));
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of(s));

        RecurringTaskSeriesMaterializer.materializeDueOccurrences(taskRepository, seriesRepository, spaceId, LocalDate.of(2026, 1, 14));

        InOrder order = inOrder(seriesRepository);
        order.verify(seriesRepository).findSchedulesBySpaceId(spaceId);
        order.verify(seriesRepository).lockForMaterialization(spaceId);
        order.verify(seriesRepository).findBySpaceId(spaceId);
    }

    private static int anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }
    @Test
    void a_backlog_beyond_the_ceiling_is_capped_and_the_series_advances_only_that_far() {
        // Anchored far enough back that the uncapped while(true) would fill the pending list
        // until the heap gave out — before a single row was written.
        RecurringTaskSeries s = new RecurringTaskSeries(seriesId, spaceId, "Sortir les poubelles", TaskPriority.MED,
            List.of(), RecurrenceInterval.DAILY, 1, RecurrenceInterval.DAILY, 0, LocalDate.of(2000, 1, 1), null,
            0, List.of(), 0, creatorId);
        when(seriesRepository.findSchedulesBySpaceId(spaceId))
            .thenReturn(List.of(scheduleOf(s)));
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of(s));

        RecurringTaskSeriesMaterializer.materializeDueOccurrences(
            taskRepository, seriesRepository, spaceId, LocalDate.of(2026, 9, 9));

        ArgumentCaptor<List<CreateTaskCommand>> batchCaptor = ArgumentCaptor.captor();
        verify(taskRepository).createAll(batchCaptor.capture());
        assertThat(batchCaptor.getValue()).hasSize(RecurringTaskSeriesMaterializer.MAX_OCCURRENCES_PER_RUN);
        // advance() records the last occurrence generated, so the next read resumes there.
        verify(seriesRepository).advance(seriesId, 0, RecurringTaskSeriesMaterializer.MAX_OCCURRENCES_PER_RUN);
    }

    @Test
    void successive_passes_keep_catching_up_from_where_the_previous_one_left_off() {
        int alreadyGenerated = RecurringTaskSeriesMaterializer.MAX_OCCURRENCES_PER_RUN;
        RecurringTaskSeries s = new RecurringTaskSeries(seriesId, spaceId, "Sortir les poubelles", TaskPriority.MED,
            List.of(), RecurrenceInterval.DAILY, 1, RecurrenceInterval.DAILY, 0, LocalDate.of(2000, 1, 1), null,
            alreadyGenerated, List.of(), 0, creatorId);
        when(seriesRepository.findSchedulesBySpaceId(spaceId))
            .thenReturn(List.of(scheduleOf(s)));
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of(s));

        RecurringTaskSeriesMaterializer.materializeDueOccurrences(
            taskRepository, seriesRepository, spaceId, LocalDate.of(2026, 9, 9));

        verify(seriesRepository).advance(seriesId, 0,
            alreadyGenerated + RecurringTaskSeriesMaterializer.MAX_OCCURRENCES_PER_RUN);
    }
    /**
     * Derives the schedule projection from the full series, so a test cannot stub the two reads
     * with values that disagree — the pre-check and the materialization loop must see the same
     * series.
     */
    private static RecurringTaskSeriesSchedule scheduleOf(RecurringTaskSeries s) {
        return new RecurringTaskSeriesSchedule(s.id(), s.intervalType(), s.intervalCount(),
            s.leadIntervalType(), s.leadIntervalCount(), s.anchorDate(), s.endDate(), s.occurrenceCount());
    }
}
