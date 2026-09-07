package com.nido.api.tasks.application.handler;

import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.tasks.domain.model.RecurrenceInterval;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListRecurringTaskSeriesHandlerTest {

    @Mock RecurringTaskSeriesRepository seriesRepository;
    private ListRecurringTaskSeriesHandler handler;
    private final UUID spaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ListRecurringTaskSeriesHandler(seriesRepository);
    }

    @Test
    void lists_the_callers_space_series() {
        SpaceMembership membership = new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());
        RecurringTaskSeries series = new RecurringTaskSeries(UUID.randomUUID(), spaceId, "T", TaskPriority.MED, List.of(),
            RecurrenceInterval.WEEKLY, 1, RecurrenceInterval.DAILY, 0, LocalDate.of(2026, 1, 7), null, 0, List.of(), 0, null);
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of(series));

        List<RecurringTaskSeries> result = handler.list(membership);

        assertThat(result).containsExactly(series);
    }
}
