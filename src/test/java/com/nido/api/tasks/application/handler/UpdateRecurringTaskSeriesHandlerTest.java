package com.nido.api.tasks.application.handler;

import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.tasks.domain.model.RecurrenceInterval;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.model.TaskException;
import com.nido.api.tasks.domain.model.TaskPriority;
import com.nido.api.tasks.domain.model.UpdateRecurringTaskSeriesCommand;
import com.nido.api.tasks.domain.port.out.RecurringTaskSeriesRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateRecurringTaskSeriesHandlerTest {

    @Mock RecurringTaskSeriesRepository seriesRepository;
    private UpdateRecurringTaskSeriesHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID seriesId = UUID.randomUUID();
    private final LocalDate anchor = LocalDate.of(2026, 1, 7);

    @BeforeEach
    void setUp() {
        handler = new UpdateRecurringTaskSeriesHandler(seriesRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    private UpdateRecurringTaskSeriesCommand command(int leadIntervalCount, LocalDate endDate) {
        return new UpdateRecurringTaskSeriesCommand(seriesId, spaceId, "Sortir les poubelles", TaskPriority.MED, List.of(),
            RecurrenceInterval.WEEKLY, 1, RecurrenceInterval.DAILY, leadIntervalCount, anchor, endDate, List.of());
    }

    private RecurringTaskSeries existing() {
        return new RecurringTaskSeries(seriesId, spaceId, "Sortir les poubelles", TaskPriority.MED, List.of(),
            RecurrenceInterval.WEEKLY, 1, RecurrenceInterval.DAILY, 0, anchor, null, 3, List.of(), 0);
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

    @Test
    void a_viewer_cannot_update_a_series() {
        assertThatThrownBy(() -> handler.update(command(2, null), membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }

    @Test
    void updating_a_series_from_another_space_is_not_found() {
        UpdateRecurringTaskSeriesCommand command = command(2, null);
        RecurringTaskSeries otherSpaceSeries = new RecurringTaskSeries(seriesId, UUID.randomUUID(), "T", TaskPriority.MED, List.of(),
            RecurrenceInterval.WEEKLY, 1, RecurrenceInterval.DAILY, 0, anchor, null, 0, List.of(), 0);
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
}
