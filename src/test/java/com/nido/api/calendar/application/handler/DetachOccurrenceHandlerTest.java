package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.service.CalendarSpaceMemberValidator;
import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.RecurrenceInterval;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.model.UpdateEventCommand;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DetachOccurrenceHandlerTest {

    private final RecurringEventSeriesRepository series = mock(RecurringEventSeriesRepository.class);
    private final CalendarEventRepository events = mock(CalendarEventRepository.class);
    private final EventExclusionRepository exclusions = mock(EventExclusionRepository.class);
    private final CalendarSpaceMemberValidator members = mock(CalendarSpaceMemberValidator.class);
    private final DetachOccurrenceHandler handler =
        new DetachOccurrenceHandler(series, events, exclusions, members);

    private final UUID spaceId = UUID.randomUUID();
    private final UUID seriesId = UUID.randomUUID();
    private final SpaceMembership caller =
        new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());

    @Test
    void refusesADateThatIsNotAnOccurrenceOfTheSeries() {
        // Weekly from Tuesday 2026-01-06; 2026-01-08 is a Thursday and no occurrence.
        when(series.findById(seriesId)).thenReturn(Optional.of(weeklySeries()));

        assertThatThrownBy(() -> handler.detach(seriesId, LocalDate.of(2026, 1, 8), content(), caller))
            .isInstanceOf(CalendarException.OccurrenceNotInSeries.class);
        verifyNoInteractions(events);
    }

    @Test
    void refusesADateBeyondTheSeriesEndDate() {
        when(series.findById(seriesId)).thenReturn(Optional.of(weeklySeriesEndingOn(LocalDate.of(2026, 1, 13))));

        assertThatThrownBy(() -> handler.detach(seriesId, LocalDate.of(2026, 1, 20), content(), caller))
            .isInstanceOf(CalendarException.OccurrenceNotInSeries.class);
    }

    @Test
    void createsADetachedEventOnTheFirstCallAndUpdatesItOnTheSecond() {
        when(series.findById(seriesId)).thenReturn(Optional.of(weeklySeries()));
        when(events.findBySeriesAndOriginalDate(seriesId, LocalDate.of(2026, 1, 13)))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(detachedEvent()));

        handler.detach(seriesId, LocalDate.of(2026, 1, 13), content(), caller);
        handler.detach(seriesId, LocalDate.of(2026, 1, 13), content(), caller);

        verify(events, times(1)).create(any());
        verify(events, times(1)).update(any());
    }

    @Test
    void clearsAnExistingExclusionSoAReinstatedOccurrenceIsNotHiddenTwice() {
        // "Cancel this week, then change your mind and edit it instead": without lifting the
        // exclusion, the new detached event would exist but stay invisible.
        when(series.findById(seriesId)).thenReturn(Optional.of(weeklySeries()));
        when(events.findBySeriesAndOriginalDate(any(), any())).thenReturn(Optional.empty());

        handler.detach(seriesId, LocalDate.of(2026, 1, 13), content(), caller);

        verify(exclusions).clear(seriesId, LocalDate.of(2026, 1, 13));
    }

    @Test
    void refusesASeriesOfAnotherSpace() {
        RecurringEventSeries foreign = new RecurringEventSeries(
            seriesId, UUID.randomUUID(), "Piano", null, null, false,
            LocalTime.of(18, 0), LocalTime.of(19, 0), 0, null,
            RecurrenceInterval.WEEKLY, 1, LocalDate.of(2026, 1, 6), null, List.of(), UUID.randomUUID(), Instant.now());
        when(series.findById(seriesId)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> handler.detach(seriesId, LocalDate.of(2026, 1, 13), content(), caller))
            .isInstanceOf(CalendarException.RecurringEventSeriesNotFound.class);
    }

    @Test
    void refusesASeriesThatDoesNotExist() {
        when(series.findById(seriesId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.detach(seriesId, LocalDate.of(2026, 1, 13), content(), caller))
            .isInstanceOf(CalendarException.RecurringEventSeriesNotFound.class);
    }

    private UpdateEventCommand content() {
        return new UpdateEventCommand(null, "Piano (décalé)", null, null, false,
            LocalDate.of(2026, 1, 15), LocalTime.of(19, 0), LocalDate.of(2026, 1, 15), LocalTime.of(20, 0),
            null, List.of());
    }

    private CalendarEvent detachedEvent() {
        return new CalendarEvent(UUID.randomUUID(), spaceId, "Piano (décalé)", null, null, false,
            LocalDate.of(2026, 1, 15), LocalTime.of(19, 0), LocalDate.of(2026, 1, 15), LocalTime.of(20, 0),
            null, List.of(), seriesId, LocalDate.of(2026, 1, 13), UUID.randomUUID(), Instant.now());
    }

    private RecurringEventSeries weeklySeries() {
        return weeklySeriesEndingOn(null);
    }

    private RecurringEventSeries weeklySeriesEndingOn(LocalDate endDate) {
        return new RecurringEventSeries(
            seriesId, spaceId, "Piano", null, null, false,
            LocalTime.of(18, 0), LocalTime.of(19, 0), 0, null,
            RecurrenceInterval.WEEKLY, 1, LocalDate.of(2026, 1, 6), endDate, List.of(), UUID.randomUUID(), Instant.now());
    }
}
