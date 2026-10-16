package com.nido.api.calendar.application.service;

import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CalendarFeed;
import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.model.RecurrenceInterval;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.calendar.domain.port.out.CalendarSource;
import com.nido.api.calendar.domain.port.out.EventExclusionRepository;
import com.nido.api.calendar.domain.port.out.RecurringEventSeriesRepository;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
import com.nido.api.space.application.port.in.GetSpaceUseCase;
import com.nido.api.space.domain.model.SpaceDetailView;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CalendarFeedAssemblerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 13);

    private final CalendarEventRepository events = mock(CalendarEventRepository.class);
    private final RecurringEventSeriesRepository series = mock(RecurringEventSeriesRepository.class);
    private final EventExclusionRepository exclusions = mock(EventExclusionRepository.class);
    private final CalendarSource tasks = mock(CalendarSource.class);
    private final CalendarSource meals = mock(CalendarSource.class);
    private final GetSpaceUseCase spaces = mock(GetSpaceUseCase.class);
    private final GetSpaceTodayUseCase today = mock(GetSpaceTodayUseCase.class);

    private final UUID spaceId = UUID.randomUUID();
    private final UUID me = UUID.randomUUID();
    private final UUID someoneElse = UUID.randomUUID();
    private final SpaceMembership reader = new SpaceMembership(UUID.randomUUID(), spaceId, me, SpaceRole.VIEWER, Instant.now());

    private CalendarFeedAssembler assembler;

    @BeforeEach
    void setUp() {
        when(tasks.type()).thenReturn(CalendarSourceType.TASK);
        when(meals.type()).thenReturn(CalendarSourceType.MEAL);
        when(spaces.get(spaceId, reader)).thenReturn(new SpaceDetailView(spaceId, SpaceType.SHARED, "Chez nous", null,
            "#c17a5c", "🏡", ZoneId.of("Europe/Paris"), SpaceRole.VIEWER, 2));
        when(today.today(spaceId)).thenReturn(TODAY);
        when(exclusions.findAllSlotsOfEach(anyCollection())).thenReturn(Map.of());
        assembler = new CalendarFeedAssembler(events, series, exclusions, List.of(tasks, meals), spaces, today);
    }

    @Test
    void theEventsAreWrittenInFullWithTheSpacesNameAndZone() {
        CalendarEvent concert = event(List.of(), null, null);
        when(events.findBySpaceId(spaceId)).thenReturn(List.of(concert));
        when(series.findBySpaceId(spaceId)).thenReturn(List.of());

        CalendarFeed feed = assembler.assemble(reader, Set.of(CalendarSourceType.EVENT), false);

        assertThat(feed.name()).isEqualTo("Chez nous");
        assertThat(feed.zone()).isEqualTo(ZoneId.of("Europe/Paris"));
        assertThat(feed.events()).containsExactly(concert);
        verify(tasks, never()).occurrencesBetween(any(), any(), any());
    }

    @Test
    void onlyWhatConcernsTheReaderKeepsEventsWithNobodyOrWithThem() {
        CalendarEvent mine = event(List.of(me), null, null);
        CalendarEvent everyones = event(List.of(), null, null);
        CalendarEvent theirs = event(List.of(someoneElse), null, null);
        when(events.findBySpaceId(spaceId)).thenReturn(List.of(mine, everyones, theirs));
        when(series.findBySpaceId(spaceId)).thenReturn(List.of());

        assertThat(assembler.assemble(reader, Set.of(CalendarSourceType.EVENT), true).events()).containsExactly(mine, everyones);
    }

    @Test
    void anOccurrenceOfMySeriesMadeSomeoneElsesIsCancelledForMe() {
        RecurringEventSeries piano = series(List.of(me));
        CalendarEvent givenAway = event(List.of(someoneElse), piano.id(), LocalDate.of(2026, 1, 12));
        when(events.findBySpaceId(spaceId)).thenReturn(List.of(givenAway));
        when(series.findBySpaceId(spaceId)).thenReturn(List.of(piano));

        CalendarFeed feed = assembler.assemble(reader, Set.of(CalendarSourceType.EVENT), true);

        assertThat(feed.series()).singleElement().satisfies(s -> {
            assertThat(s.cancelledSlots()).containsExactly(LocalDate.of(2026, 1, 12));
            assertThat(s.editedOccurrences()).isEmpty();
        });
    }

    @Test
    void anOccurrenceOfSomeoneElsesSeriesMadeMineIsMyOwnEvent() {
        RecurringEventSeries football = series(List.of(someoneElse));
        CalendarEvent joined = event(List.of(me), football.id(), LocalDate.of(2026, 1, 12));
        when(events.findBySpaceId(spaceId)).thenReturn(List.of(joined));
        when(series.findBySpaceId(spaceId)).thenReturn(List.of(football));

        CalendarFeed feed = assembler.assemble(reader, Set.of(CalendarSourceType.EVENT), true);

        assertThat(feed.series()).isEmpty();
        assertThat(feed.events()).containsExactly(joined);
    }

    @Test
    void theOtherSourcesAreReadFromThreeMonthsBackToAYearAheadInWindowsACalendarReadAllows() {
        when(tasks.occurrencesBetween(eq(reader), any(), any())).thenReturn(List.of());

        assembler.assemble(reader, Set.of(CalendarSourceType.TASK), false);

        verify(tasks).occurrencesBetween(reader, LocalDate.of(2026, 7, 13), LocalDate.of(2027, 7, 13));
        verify(tasks).occurrencesBetween(reader, LocalDate.of(2027, 7, 14), LocalDate.of(2027, 10, 13));
        verify(meals, never()).occurrencesBetween(any(), any(), any());
    }

    @Test
    void onlyWhatConcernsTheReaderKeepsTheTasksAssignedToThemOrToNobody() {
        CalendarOccurrence mine = task("t1", List.of(me));
        CalendarOccurrence nobodys = task("t2", List.of());
        CalendarOccurrence theirs = task("t3", List.of(someoneElse));
        when(tasks.occurrencesBetween(eq(reader), any(), any())).thenReturn(List.of(mine, nobodys, theirs), List.of());

        assertThat(assembler.assemble(reader, Set.of(CalendarSourceType.TASK), true).others()).containsExactly(mine, nobodys);
    }

    private CalendarEvent event(List<UUID> participants, UUID seriesId, LocalDate slot) {
        return new CalendarEvent(UUID.randomUUID(), spaceId, "Concert", null, null, true,
            slot == null ? LocalDate.of(2026, 9, 23) : slot, null, slot == null ? LocalDate.of(2026, 9, 23) : slot, null,
            null, participants, seriesId, slot, UUID.randomUUID(), Instant.now());
    }

    private RecurringEventSeries series(List<UUID> participants) {
        return new RecurringEventSeries(UUID.randomUUID(), spaceId, "Piano", null, null, false, LocalTime.of(18, 0),
            LocalTime.of(19, 0), 0, null, RecurrenceInterval.WEEKLY, 1, LocalDate.of(2026, 1, 5), null, participants,
            UUID.randomUUID(), Instant.now());
    }

    private CalendarOccurrence task(String id, List<UUID> assignees) {
        return new CalendarOccurrence(CalendarSourceType.TASK, id, null, null, true, "Tâche " + id, null, null, true,
            TODAY, null, TODAY, null, null, assignees);
    }
}
