package com.nido.api.tasks.application.handler;

import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.tasks.domain.model.RecurrenceInterval;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.model.TaskException;
import com.nido.api.tasks.domain.model.TaskPriority;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteRecurringTaskSeriesHandlerTest {

    @Mock RecurringTaskSeriesRepository seriesRepository;
    private DeleteRecurringTaskSeriesHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID seriesId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new DeleteRecurringTaskSeriesHandler(seriesRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    private RecurringTaskSeries series(UUID inSpaceId) {
        return new RecurringTaskSeries(seriesId, inSpaceId, "T", TaskPriority.MED, List.of(),
            RecurrenceInterval.WEEKLY, 1, RecurrenceInterval.DAILY, 0, LocalDate.of(2026, 1, 7), null, 0, List.of(), 0, null);
    }

    @Test
    void a_member_can_delete_a_series_in_their_space() {
        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(series(spaceId)));

        handler.delete(seriesId, spaceId, membership(SpaceRole.MEMBER));

        verify(seriesRepository).lockForMaterialization(spaceId);
        verify(seriesRepository).deleteById(seriesId);
    }

    @Test
    void a_viewer_cannot_delete_a_series() {
        assertThatThrownBy(() -> handler.delete(seriesId, spaceId, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }

    @Test
    void deleting_a_series_from_another_space_is_not_found() {
        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(series(UUID.randomUUID())));

        assertThatThrownBy(() -> handler.delete(seriesId, spaceId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(TaskException.RecurringSeriesNotFound.class);
    }

    @Test
    void deleting_a_nonexistent_series_is_not_found() {
        when(seriesRepository.findById(seriesId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.delete(seriesId, spaceId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(TaskException.RecurringSeriesNotFound.class);
    }
}
