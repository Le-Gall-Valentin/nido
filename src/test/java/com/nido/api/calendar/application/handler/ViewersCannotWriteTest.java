package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.service.CalendarSpaceMemberValidator;
import com.nido.api.calendar.application.service.SeriesExceptionsMover;
import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CreateEventCommand;
import com.nido.api.calendar.domain.model.CreateRecurringEventSeriesCommand;
import com.nido.api.calendar.domain.model.RecurrenceInterval;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.model.UpdateEventCommand;
import com.nido.api.calendar.domain.model.UpdateRecurringEventSeriesCommand;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.calendar.domain.port.out.EventExclusionRepository;
import com.nido.api.calendar.domain.port.out.RecurringEventSeriesRepository;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Every write refuses a VIEWER in the use case itself, as in the other modules — not only through the
 * route's role floor, which a caller that is not a controller would never cross.
 */
class ViewersCannotWriteTest {

    private final CalendarEventRepository events = mock(CalendarEventRepository.class);
    private final RecurringEventSeriesRepository series = mock(RecurringEventSeriesRepository.class);
    private final EventExclusionRepository exclusions = mock(EventExclusionRepository.class);
    private final CalendarSpaceMemberValidator rule = mock(CalendarSpaceMemberValidator.class);
    private final GetSpaceTodayUseCase spaceToday = mock(GetSpaceTodayUseCase.class);
    private final SeriesExceptionsMover mover = mock(SeriesExceptionsMover.class);

    private final UUID spaceId = UUID.randomUUID();
    private final SpaceMembership viewer =
        new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.VIEWER, Instant.now());
    private final LocalDate day = LocalDate.of(2026, 10, 5);
    private final UUID eventId = UUID.randomUUID();
    private final RecurringEventSeries weekly = new RecurringEventSeries(UUID.randomUUID(), spaceId, "Piano", null, null,
        true, null, null, 0, null, RecurrenceInterval.WEEKLY, 1, day, null, List.of(), UUID.randomUUID(), Instant.now());

    @BeforeEach
    void everythingElseWouldLetTheWriteThrough() {
        when(events.findById(eventId)).thenReturn(Optional.of(new CalendarEvent(eventId, spaceId, "Dentiste", null, null,
            true, day, null, day, null, null, List.of(), null, null, UUID.randomUUID(), Instant.now())));
        when(series.findById(weekly.id())).thenReturn(Optional.of(weekly));
        when(events.findBySeriesAndOriginalDate(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void creatingAnEvent() {
        refused(() -> new CreateEventHandler(events, rule).create(new CreateEventCommand(spaceId, "Dentiste", null, null,
            true, day, null, day, null, null, List.of(), null, null, viewer.userId()), viewer));
    }

    @Test
    void editingAnEvent() {
        refused(() -> new UpdateEventHandler(events, rule).update(new UpdateEventCommand(eventId, "Dentiste", null, null,
            true, day, null, day, null, null, List.of()), viewer));
    }

    @Test
    void deletingAnEvent() {
        refused(() -> new DeleteEventHandler(events, exclusions).delete(eventId, viewer));
    }

    @Test
    void joiningAnEvent() {
        refused(() -> new JoinEventHandler(events, rule).join(eventId, viewer));
    }

    @Test
    void leavingAnEvent() {
        refused(() -> new LeaveEventHandler(events, rule).leave(eventId, viewer));
    }

    @Test
    void creatingASeries() {
        refused(() -> new CreateRecurringEventSeriesHandler(series, rule).create(new CreateRecurringEventSeriesCommand(
            spaceId, "Piano", null, null, true, null, null, 0, null, RecurrenceInterval.WEEKLY, 1, day, null, List.of(),
            viewer.userId()), viewer));
    }

    @Test
    void editingASeries() {
        refused(() -> new UpdateRecurringEventSeriesHandler(series, rule, spaceToday, mover).update(new UpdateRecurringEventSeriesCommand(
            weekly.id(), "Piano", null, null, true, null, null, 0, null, RecurrenceInterval.WEEKLY, 1, day, null,
            List.of()), viewer));
    }

    @Test
    void deletingASeries() {
        refused(() -> new DeleteRecurringEventSeriesHandler(series, spaceToday, mover).delete(weekly.id(), viewer));
    }

    @Test
    void editingOneOccurrence() {
        refused(() -> new DetachOccurrenceHandler(series, events, exclusions, rule).detach(weekly.id(), day,
            new UpdateEventCommand(null, "Piano", null, null, true, day, null, day, null, null, List.of()), viewer));
    }

    @Test
    void cancellingOneOccurrence() {
        refused(() -> new ExcludeOccurrenceHandler(series, events, exclusions).exclude(weekly.id(), day, viewer));
    }

    /** Refused for its role — and refused before anything was read, checked or written. */
    private void refused(ThrowingCallable write) {
        assertThatThrownBy(write).isInstanceOf(SpaceException.InsufficientRole.class);
        verifyNoInteractions(rule, exclusions, mover);
    }
}
