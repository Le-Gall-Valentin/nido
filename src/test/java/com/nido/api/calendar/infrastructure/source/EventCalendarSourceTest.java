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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
        when(exclusions.findSlotsOfEach(any(), any(), any())).thenReturn(Map.of());
        when(events.findDetachedSlotsOfEach(any(), any(), any())).thenReturn(Map.of(seriesId, Set.of(LocalDate.of(2026, 1, 13))));

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
        when(exclusions.findSlotsOfEach(any(), any(), any())).thenReturn(Map.of(seriesId, Set.of(LocalDate.of(2026, 1, 13))));
        when(events.findDetachedSlotsOfEach(any(), any(), any())).thenReturn(Map.of());

        assertThat(source.occurrencesBetween(caller, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 20)))
            .extracting(CalendarOccurrence::startDate)
            .containsExactly(LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 20));
    }

    @Test
    void marksStoredEventsMaterializedAndProjectedOnesNot() {
        when(events.findBySpaceIdOverlapping(eq(spaceId), any(), any()))
            .thenReturn(List.of(event(LocalDate.of(2026, 1, 2), null, null)));
        when(series.findBySpaceId(spaceId)).thenReturn(List.of(weekly()));
        when(exclusions.findSlotsOfEach(any(), any(), any())).thenReturn(Map.of());
        when(events.findDetachedSlotsOfEach(any(), any(), any())).thenReturn(Map.of());

        List<CalendarOccurrence> produced =
            source.occurrencesBetween(caller, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 7));

        assertThat(produced).filteredOn(o -> o.startDate().equals(LocalDate.of(2026, 1, 2)))
            .singleElement().extracting(CalendarOccurrence::materialized).isEqualTo(true);
        assertThat(produced).filteredOn(o -> o.startDate().equals(LocalDate.of(2026, 1, 6)))
            .singleElement().extracting(CalendarOccurrence::materialized).isEqualTo(false);
    }

    @Test
    void carriesAStoredEventsDescriptionAndLocation() {
        CalendarEvent stored = new CalendarEvent(UUID.randomUUID(), spaceId, "Concert", "Apporter les billets",
            "Salle Pleyel", true, LocalDate.of(2026, 1, 2), null, LocalDate.of(2026, 1, 2), null,
            null, List.of(), null, null, UUID.randomUUID(), Instant.now());
        when(events.findBySpaceIdOverlapping(eq(spaceId), any(), any())).thenReturn(List.of(stored));
        when(series.findBySpaceId(spaceId)).thenReturn(List.of());

        CalendarOccurrence produced =
            source.occurrencesBetween(caller, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 7)).getFirst();

        assertThat(produced.description()).isEqualTo("Apporter les billets");
        assertThat(produced.location()).isEqualTo("Salle Pleyel");
    }

    // A weekend away, Friday to Sunday every week from Fri 2026-01-09. Looked at from Saturday the
    // 17th, the occurrence of Friday the 16th is still running, so the projector reaches back to it.
    // The repositories answer like the real queries: only for slots inside the range they are asked.

    @Test
    void keepsACancelledOccurrenceCancelledWhenItStartedBeforeTheWindow() {
        LocalDate cancelled = LocalDate.of(2026, 1, 16);
        when(events.findBySpaceIdOverlapping(eq(spaceId), any(), any())).thenReturn(List.of());
        when(series.findBySpaceId(spaceId)).thenReturn(List.of(weekend()));
        when(exclusions.findSlotsOfEach(any(), any(), any()))
            .thenAnswer(ask -> Map.of(seriesId, slotsIn(ask.getArgument(1), ask.getArgument(2), cancelled)));
        when(events.findDetachedSlotsOfEach(any(), any(), any())).thenReturn(Map.of());

        assertThat(source.occurrencesBetween(caller, LocalDate.of(2026, 1, 17), LocalDate.of(2026, 1, 25)))
            .extracting(CalendarOccurrence::startDate)
            .containsExactly(LocalDate.of(2026, 1, 23));
    }

    @Test
    void neverShowsAMovedOccurrenceTwiceWhenItsSlotStartedBeforeTheWindow() {
        // The weekend of the 16th was moved a day later: it now runs Saturday 17 to Monday 19.
        LocalDate slot = LocalDate.of(2026, 1, 16);
        CalendarEvent moved = new CalendarEvent(UUID.randomUUID(), spaceId, "Week-end", null, null, true,
            LocalDate.of(2026, 1, 17), null, LocalDate.of(2026, 1, 19), null,
            null, List.of(), seriesId, slot, UUID.randomUUID(), Instant.now());
        when(events.findBySpaceIdOverlapping(eq(spaceId), any(), any())).thenReturn(List.of(moved));
        when(series.findBySpaceId(spaceId)).thenReturn(List.of(weekend()));
        when(exclusions.findSlotsOfEach(any(), any(), any())).thenReturn(Map.of());
        when(events.findDetachedSlotsOfEach(any(), any(), any()))
            .thenAnswer(ask -> Map.of(seriesId, slotsIn(ask.getArgument(1), ask.getArgument(2), slot)));

        assertThat(source.occurrencesBetween(caller, LocalDate.of(2026, 1, 17), LocalDate.of(2026, 1, 25)))
            .extracting(CalendarOccurrence::startDate)
            .containsExactlyInAnyOrder(LocalDate.of(2026, 1, 17), LocalDate.of(2026, 1, 23));
    }

    private static Set<LocalDate> slotsIn(LocalDate from, LocalDate to, LocalDate slot) {
        return slot.isBefore(from) || slot.isAfter(to) ? Set.of() : Set.of(slot);
    }

    private RecurringEventSeries weekend() {
        return new RecurringEventSeries(
            seriesId, spaceId, "Week-end", null, null, true,
            null, null, 2, null,
            RecurrenceInterval.WEEKLY, 1, LocalDate.of(2026, 1, 9), null, List.of(), UUID.randomUUID(), Instant.now());
    }

    @Test
    void readsTheCancelledAndEditedSlotsOfEverySeriesInOneQueryEach() {
        // Two queries per series per read, before: a space's every series cost a pair on every page.
        RecurringEventSeries solfege = new RecurringEventSeries(UUID.randomUUID(), spaceId, "Solfège", null, null, false,
            LocalTime.of(17, 0), LocalTime.of(18, 0), 0, null, RecurrenceInterval.WEEKLY, 1, LocalDate.of(2026, 1, 7),
            null, List.of(), UUID.randomUUID(), Instant.now());
        when(events.findBySpaceIdOverlapping(eq(spaceId), any(), any())).thenReturn(List.of());
        when(series.findBySpaceId(spaceId)).thenReturn(List.of(weekly(), solfege));
        when(exclusions.findSlotsOfEach(any(), any(), any())).thenReturn(Map.of(solfege.id(), Set.of(LocalDate.of(2026, 1, 14))));
        when(events.findDetachedSlotsOfEach(any(), any(), any())).thenReturn(Map.of());

        List<CalendarOccurrence> produced = source.occurrencesBetween(caller, LocalDate.of(2026, 1, 12), LocalDate.of(2026, 1, 18));

        // Each series' own cancellations apply to it alone: Solfège's 14th is gone, Piano's 13th stays.
        assertThat(produced).extracting(CalendarOccurrence::title).containsExactly("Piano");
        verify(exclusions, times(1)).findSlotsOfEach(any(), any(), any());
        verify(events, times(1)).findDetachedSlotsOfEach(any(), any(), any());
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
