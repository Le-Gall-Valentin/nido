package com.nido.api.calendar.infrastructure.source;

import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.model.RecurrenceInterval;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.calendar.domain.port.out.EventExclusionRepository;
import com.nido.api.calendar.domain.port.out.RecurringEventSeriesRepository;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventCalendarSourceTest {

    private final CalendarEventRepository events = mock(CalendarEventRepository.class);
    private final RecurringEventSeriesRepository series = mock(RecurringEventSeriesRepository.class);
    private final EventExclusionRepository exclusions = mock(EventExclusionRepository.class);
    private final EventCalendarSource source = new EventCalendarSource(events, series, exclusions);

    private final UUID spaceId = UUID.randomUUID();
    private final UUID seriesId = UUID.randomUUID();
    private final SpaceMembership caller =
        new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());

    @Test
    void returnsPlainEventsAndProjectedOccurrencesWithoutShowingADetachedInstanceTwice() {
        // Weekly from 2026-01-06; the 01-13 slot was edited and now sits on 01-15.
        CalendarEvent detached = event(LocalDate.of(2026, 1, 15), seriesId, LocalDate.of(2026, 1, 13));
        when(events.findBySpaceIdOverlapping(eq(spaceId), any(), any())).thenReturn(List.of(detached));
        when(series.findBySpaceId(spaceId)).thenReturn(List.of(weekly()));
        when(exclusions.findSlots(eq(seriesId), any(), any())).thenReturn(Set.of());
        when(events.findDetachedSlots(eq(seriesId), any(), any())).thenReturn(Set.of(LocalDate.of(2026, 1, 13)));

        List<CalendarOccurrence> produced =
            source.occurrencesBetween(caller, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 20));

        assertThat(produced).extracting(CalendarOccurrence::startDate)
            .containsExactlyInAnyOrder(
                LocalDate.of(2026, 1, 6),    // projected
                LocalDate.of(2026, 1, 15),   // the detached instance, at its NEW date
                LocalDate.of(2026, 1, 20));  // projected
        assertThat(produced).noneMatch(o -> o.startDate().equals(LocalDate.of(2026, 1, 13)));
    }

    @Test
    void dropsAnExcludedSlotEntirely() {
        when(events.findBySpaceIdOverlapping(eq(spaceId), any(), any())).thenReturn(List.of());
        when(series.findBySpaceId(spaceId)).thenReturn(List.of(weekly()));
        when(exclusions.findSlots(eq(seriesId), any(), any())).thenReturn(Set.of(LocalDate.of(2026, 1, 13)));
        when(events.findDetachedSlots(eq(seriesId), any(), any())).thenReturn(Set.of());

        assertThat(source.occurrencesBetween(caller, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 20)))
            .extracting(CalendarOccurrence::startDate)
            .containsExactly(LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 20));
    }

    @Test
    void marksStoredEventsMaterializedAndProjectedOnesNot() {
        when(events.findBySpaceIdOverlapping(eq(spaceId), any(), any()))
            .thenReturn(List.of(event(LocalDate.of(2026, 1, 2), null, null)));
        when(series.findBySpaceId(spaceId)).thenReturn(List.of(weekly()));
        when(exclusions.findSlots(eq(seriesId), any(), any())).thenReturn(Set.of());
        when(events.findDetachedSlots(eq(seriesId), any(), any())).thenReturn(Set.of());

        List<CalendarOccurrence> produced =
            source.occurrencesBetween(caller, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 7));

        assertThat(produced).filteredOn(o -> o.startDate().equals(LocalDate.of(2026, 1, 2)))
            .singleElement().extracting(CalendarOccurrence::materialized).isEqualTo(true);
        assertThat(produced).filteredOn(o -> o.startDate().equals(LocalDate.of(2026, 1, 6)))
            .singleElement().extracting(CalendarOccurrence::materialized).isEqualTo(false);
    }

    @Test
    void reportsItselfAsTheEventSource() {
        assertThat(source.type()).isEqualTo(CalendarSourceType.EVENT);
    }

    private RecurringEventSeries weekly() {
        return new RecurringEventSeries(
            seriesId, spaceId, "Piano", null, null, false,
            LocalTime.of(18, 0), LocalTime.of(19, 0), 0, null,
            RecurrenceInterval.WEEKLY, 1, LocalDate.of(2026, 1, 6), null, List.of(), UUID.randomUUID(), Instant.now());
    }

    private CalendarEvent event(LocalDate date, UUID linkedSeriesId, LocalDate originalDate) {
        return new CalendarEvent(UUID.randomUUID(), spaceId, "Piano", null, null, false,
            date, LocalTime.of(19, 0), date, LocalTime.of(20, 0),
            null, List.of(), linkedSeriesId, originalDate, UUID.randomUUID(), Instant.now());
    }
}
