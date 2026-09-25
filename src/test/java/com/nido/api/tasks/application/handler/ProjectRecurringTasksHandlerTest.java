package com.nido.api.tasks.application.handler;

import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.tasks.domain.model.ProjectedTaskOccurrence;
import com.nido.api.tasks.domain.model.RecurrenceInterval;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.model.TaskPriority;
import com.nido.api.tasks.domain.port.out.RecurringTaskSeriesRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectRecurringTasksHandlerTest {

    private final RecurringTaskSeriesRepository series = mock(RecurringTaskSeriesRepository.class);
    private final ProjectRecurringTasksHandler handler = new ProjectRecurringTasksHandler(series);

    private final UUID spaceId = UUID.randomUUID();
    private final SpaceMembership caller =
        new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());

    // occurrenceCount is the number of the last occurrence generated, not how many were: creating a
    // series generates occurrence 0 (the anchor) with a count of 0, and the materializer generates
    // count+1 onwards and stores the last one it generated. So occurrences 0..count all exist as
    // real tasks — the calendar lists them from the task read, and projecting any of them again
    // shows it twice.

    @Test
    void neverProjectsTheTaskCreatedWithTheSeries() {
        when(series.findBySpaceId(spaceId)).thenReturn(List.of(weekly(LocalDate.of(2026, 1, 1), 0, null)));

        assertThat(handler.project(caller, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
            .extracting(ProjectedTaskOccurrence::dueDate)
            .containsExactly(LocalDate.of(2026, 1, 8), LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 1, 22), LocalDate.of(2026, 1, 29));
    }

    @Test
    void neverProjectsAnOccurrenceAlreadyGenerated() {
        // Weekly from Jan 1, last generated occurrence number 2: Jan 1, 8 and 15 are real tasks.
        when(series.findBySpaceId(spaceId)).thenReturn(List.of(weekly(LocalDate.of(2026, 1, 1), 2, null)));

        assertThat(handler.project(caller, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
            .extracting(ProjectedTaskOccurrence::dueDate)
            .containsExactly(LocalDate.of(2026, 1, 22), LocalDate.of(2026, 1, 29));
    }

    @Test
    void projectsNothingForASeriesWhoseNextOccurrenceIsBeyondTheWindow() {
        when(series.findBySpaceId(spaceId)).thenReturn(List.of(weekly(LocalDate.of(2026, 6, 1), 0, null)));

        assertThat(handler.project(caller, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))).isEmpty();
    }

    @Test
    void stopsAtTheSeriesEndDate() {
        when(series.findBySpaceId(spaceId))
            .thenReturn(List.of(weekly(LocalDate.of(2026, 1, 1), 0, LocalDate.of(2026, 1, 10))));

        assertThat(handler.project(caller, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
            .extracting(ProjectedTaskOccurrence::dueDate)
            .containsExactly(LocalDate.of(2026, 1, 8));
    }

    @Test
    void carriesTheSeriesTitleAndPriority() {
        when(series.findBySpaceId(spaceId)).thenReturn(List.of(weekly(LocalDate.of(2026, 1, 1), 0, null)));

        assertThat(handler.project(caller, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 8)))
            .singleElement()
            .extracting(ProjectedTaskOccurrence::title, ProjectedTaskOccurrence::priority)
            .containsExactly("Sortir les poubelles", TaskPriority.MED);
    }

    private RecurringTaskSeries weekly(LocalDate anchor, int occurrenceCount, LocalDate endDate) {
        return new RecurringTaskSeries(
            UUID.randomUUID(), spaceId, "Sortir les poubelles", TaskPriority.MED, List.of(),
            RecurrenceInterval.WEEKLY, 1, RecurrenceInterval.DAILY, 0,
            anchor, endDate, occurrenceCount, List.of(), 0, UUID.randomUUID());
    }
}
