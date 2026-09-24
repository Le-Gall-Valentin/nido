package com.nido.api.tasks.application.handler;

import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.tasks.application.service.TaskSpaceMemberValidator;
import com.nido.api.tasks.domain.model.RecurrenceInterval;
import com.nido.api.tasks.domain.model.RecurrenceScheduler;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.model.TaskException;
import com.nido.api.tasks.domain.model.TaskPriority;
import com.nido.api.tasks.domain.model.UpdateRecurringTaskSeriesCommand;
import com.nido.api.tasks.domain.port.out.RecurringTaskSeriesRepository;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateRecurringTaskSeriesHandlerTest {

    @Mock RecurringTaskSeriesRepository seriesRepository;

    @Mock GetSpaceTodayUseCase spaceToday;
    @Mock TaskSpaceMemberValidator spaceMemberValidator;
    private UpdateRecurringTaskSeriesHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID seriesId = UUID.randomUUID();
    private final LocalDate anchor = LocalDate.of(2026, 1, 7);

    @BeforeEach
    void setUp() {
        // The space's date, not the server's: these handlers validate a backlog against
        // "today" and the space is what decides which day that is.
        lenient().when(spaceToday.today(any())).thenReturn(LocalDate.of(2026, 9, 12));
        handler = new UpdateRecurringTaskSeriesHandler(seriesRepository, spaceMemberValidator, spaceToday);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    private UpdateRecurringTaskSeriesCommand command(int leadIntervalCount, LocalDate endDate) {
        return command(leadIntervalCount, endDate, List.of());
    }

    private UpdateRecurringTaskSeriesCommand command(int leadIntervalCount, LocalDate endDate, List<UUID> rotationMemberIds) {
        return new UpdateRecurringTaskSeriesCommand(seriesId, spaceId, "Sortir les poubelles", TaskPriority.MED, List.of(),
            RecurrenceInterval.WEEKLY, 1, RecurrenceInterval.DAILY, leadIntervalCount, anchor, endDate, rotationMemberIds);
    }

    private RecurringTaskSeries existing() {
        return new RecurringTaskSeries(seriesId, spaceId, "Sortir les poubelles", TaskPriority.MED, List.of(),
            RecurrenceInterval.WEEKLY, 1, RecurrenceInterval.DAILY, 0, anchor, null, 3, List.of(), 0, null);
    }

    @Test
    void a_member_can_update_a_series_in_their_space() {
        UpdateRecurringTaskSeriesCommand command = command(2, null);
        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(existing()));
        RecurringTaskSeries updated = existing();
        when(seriesRepository.update(command)).thenReturn(updated);

        RecurringTaskSeries result = handler.update(command, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(updated);
    }

    // A running weekly series from Jan 7 whose last generated occurrence is number 2: the Jan 7, 14 and
    // 21 tasks exist. Its start date is then edited, the frequency left alone. The count must follow
    // the new start, or the next task generated lands on a date that already has one.

    private RecurringTaskSeries runningSince(LocalDate start, int lastGenerated) {
        return new RecurringTaskSeries(seriesId, spaceId, "Sortir les poubelles", TaskPriority.MED, List.of(),
            RecurrenceInterval.WEEKLY, 1, RecurrenceInterval.DAILY, 0, start, null, lastGenerated, List.of(), 0, null);
    }

    private UpdateRecurringTaskSeriesCommand startingOn(LocalDate start) {
        return startingOn(start, RecurrenceInterval.WEEKLY);
    }

    private UpdateRecurringTaskSeriesCommand startingOn(LocalDate start, RecurrenceInterval every) {
        return new UpdateRecurringTaskSeriesCommand(seriesId, spaceId, "Sortir les poubelles", TaskPriority.MED, List.of(),
            every, 1, RecurrenceInterval.DAILY, 0, start, null, List.of());
    }

    /** The next task the materializer would generate once the edit is saved. */
    private LocalDate nextTaskAfterEdit(LocalDate newStart) {
        return nextTaskAfterEdit(newStart, RecurrenceInterval.WEEKLY);
    }

    private LocalDate nextTaskAfterEdit(LocalDate newStart, RecurrenceInterval every) {
        ArgumentCaptor<Integer> count = ArgumentCaptor.forClass(Integer.class);
        verify(seriesRepository).advance(eq(seriesId), anyInt(), count.capture());
        return RecurrenceScheduler.nextDueDate(newStart, every, 1, count.getValue() + 1);
    }

    @Test
    void moving_the_start_of_a_running_series_earlier_never_generates_a_task_that_already_exists() {
        LocalDate newStart = LocalDate.of(2025, 12, 31);
        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(runningSince(anchor, 2)));
        when(seriesRepository.advance(eq(seriesId), anyInt(), anyInt())).thenReturn(runningSince(newStart, 3));

        handler.update(startingOn(newStart), membership(SpaceRole.MEMBER), LocalDate.of(2026, 1, 22));

        // Dec 31 + 3 weeks is Jan 21, which exists: the next one is Jan 28, not a second Jan 21.
        assertThat(nextTaskAfterEdit(newStart)).isEqualTo(LocalDate.of(2026, 1, 28));
    }

    @Test
    void moving_the_start_of_a_running_series_later_neither_repeats_nor_back_fills() {
        LocalDate newStart = LocalDate.of(2026, 1, 10);
        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(runningSince(anchor, 2)));
        when(seriesRepository.advance(eq(seriesId), anyInt(), anyInt())).thenReturn(runningSince(newStart, 1));

        handler.update(startingOn(newStart), membership(SpaceRole.MEMBER), LocalDate.of(2026, 1, 22));

        // Jan 10 and 17 fall before the last task (Jan 21): they are not generated after the fact.
        assertThat(nextTaskAfterEdit(newStart)).isEqualTo(LocalDate.of(2026, 1, 24));
    }

    @Test
    void moving_the_start_past_the_last_task_makes_that_start_the_next_task() {
        LocalDate newStart = LocalDate.of(2026, 2, 2);
        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(runningSince(anchor, 2)));
        when(seriesRepository.advance(eq(seriesId), anyInt(), anyInt())).thenReturn(runningSince(newStart, -1));

        handler.update(startingOn(newStart), membership(SpaceRole.MEMBER), LocalDate.of(2026, 1, 22));

        assertThat(nextTaskAfterEdit(newStart)).isEqualTo(newStart);
    }

    @Test
    void moving_the_start_back_after_it_was_moved_past_the_last_task_never_regenerates_one() {
        // Moved to Feb 2 earlier, past the last task (Jan 21): nothing generated since. Every task it
        // generated before falls before Feb 2, so moving the start back to Jan 14 must not reach
        // behind Feb 1 — Jan 21 in particular.
        LocalDate newStart = LocalDate.of(2026, 1, 14);
        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(runningSince(LocalDate.of(2026, 2, 2), -1)));
        when(seriesRepository.advance(eq(seriesId), anyInt(), anyInt())).thenReturn(runningSince(newStart, 2));

        handler.update(startingOn(newStart), membership(SpaceRole.MEMBER), LocalDate.of(2026, 1, 22));

        assertThat(nextTaskAfterEdit(newStart)).isEqualTo(LocalDate.of(2026, 2, 4));
    }

    @Test
    void changing_the_frequency_of_a_series_moved_past_its_last_task_starts_on_the_chosen_start() {
        // Nothing has been generated since the start was moved to Feb 2, so there is no task to
        // re-anchor on: the new frequency starts where the reader said.
        LocalDate start = LocalDate.of(2026, 2, 2);
        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(runningSince(start, -1)));
        when(seriesRepository.advance(eq(seriesId), anyInt(), anyInt())).thenReturn(runningSince(start, -1));

        handler.update(startingOn(start, RecurrenceInterval.DAILY), membership(SpaceRole.MEMBER), LocalDate.of(2026, 1, 22));

        verify(seriesRepository).update(startingOn(start, RecurrenceInterval.DAILY));
        assertThat(nextTaskAfterEdit(start, RecurrenceInterval.DAILY)).isEqualTo(start);
    }

    @Test
    void a_viewer_cannot_update_a_series() {
        assertThatThrownBy(() -> handler.update(command(2, null), membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }

    @Test
    void updating_a_series_from_another_space_is_not_found() {
        UpdateRecurringTaskSeriesCommand command = command(2, null);
        RecurringTaskSeries otherSpaceSeries = new RecurringTaskSeries(seriesId, UUID.randomUUID(), "T", TaskPriority.MED, List.of(),
            RecurrenceInterval.WEEKLY, 1, RecurrenceInterval.DAILY, 0, anchor, null, 0, List.of(), 0, null);
        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(otherSpaceSeries));

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(TaskException.RecurringSeriesNotFound.class);
    }

    @Test
    void updating_a_nonexistent_series_is_not_found() {
        UpdateRecurringTaskSeriesCommand command = command(2, null);
        when(seriesRepository.findById(seriesId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(TaskException.RecurringSeriesNotFound.class);
    }

    @Test
    void a_lead_time_longer_than_the_recurrence_interval_is_rejected() {
        UpdateRecurringTaskSeriesCommand command = command(8, null);

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(TaskException.LeadTimeExceedsInterval.class);
        verify(seriesRepository, never()).update(any());
    }

    @Test
    void an_end_date_before_the_anchor_date_is_rejected() {
        UpdateRecurringTaskSeriesCommand command = command(2, anchor.minusDays(1));

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(TaskException.InvalidEndDate.class);
        verify(seriesRepository, never()).update(any());
    }

    @Test
    void changing_the_recurrence_frequency_reanchors_the_series_on_the_last_generated_occurrence() {
        // existing() is WEEKLY/1 anchored 2026-01-07 with 3 occurrences already generated,
        // so the last generated occurrence is due 2026-01-28 (anchor + 3 weeks).
        RecurringTaskSeries existing = existing();
        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(existing));
        UpdateRecurringTaskSeriesCommand command = new UpdateRecurringTaskSeriesCommand(seriesId, spaceId,
            "Sortir les poubelles", TaskPriority.MED, List.of(), RecurrenceInterval.YEARLY, 1,
            RecurrenceInterval.DAILY, 0, anchor, null, List.of());
        UpdateRecurringTaskSeriesCommand expectedReanchoredCommand = new UpdateRecurringTaskSeriesCommand(seriesId, spaceId,
            "Sortir les poubelles", TaskPriority.MED, List.of(), RecurrenceInterval.YEARLY, 1,
            RecurrenceInterval.DAILY, 0, LocalDate.of(2026, 1, 28), null, List.of());
        RecurringTaskSeries updated = existing();
        when(seriesRepository.update(expectedReanchoredCommand)).thenReturn(updated);
        RecurringTaskSeries advanced = existing();
        when(seriesRepository.advance(seriesId, 0, 0)).thenReturn(advanced);

        RecurringTaskSeries result = handler.update(command, membership(SpaceRole.MEMBER));

        verify(seriesRepository).update(expectedReanchoredCommand);
        verify(seriesRepository).advance(seriesId, 0, 0);
        assertThat(result).isEqualTo(advanced);
    }

    @Test
    void a_rotation_member_who_is_not_in_the_space_is_rejected() {
        UUID stranger = UUID.randomUUID();
        UpdateRecurringTaskSeriesCommand command = command(0, null, List.of(stranger));
        doThrow(new TaskException.MemberNotInSpace()).when(spaceMemberValidator).ensureMember(spaceId, stranger);

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(TaskException.MemberNotInSpace.class);
        verify(seriesRepository, never()).update(any());
    }
}
