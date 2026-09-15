package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.calendar.domain.port.out.EventExclusionRepository;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DeleteEventHandlerTest {

    private final CalendarEventRepository events = mock(CalendarEventRepository.class);
    private final EventExclusionRepository exclusions = mock(EventExclusionRepository.class);
    private final DeleteEventHandler handler = new DeleteEventHandler(events, exclusions);

    private final UUID spaceId = UUID.randomUUID();
    private final SpaceMembership caller =
        new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());

    @Test
    void deletingADetachedOccurrenceAlsoExcludesItsSlotSoItDoesNotComeBack() {
        UUID eventId = UUID.randomUUID();
        UUID seriesId = UUID.randomUUID();
        LocalDate slot = LocalDate.of(2026, 2, 3);
        when(events.findById(eventId)).thenReturn(Optional.of(event(eventId, spaceId, seriesId, slot)));

        handler.delete(eventId, caller);

        verify(events).delete(eventId);
        verify(exclusions).exclude(seriesId, slot);
    }

    @Test
    void deletingAPlainEventExcludesNothing() {
        UUID eventId = UUID.randomUUID();
        when(events.findById(eventId)).thenReturn(Optional.of(event(eventId, spaceId, null, null)));

        handler.delete(eventId, caller);

        verify(events).delete(eventId);
        verifyNoInteractions(exclusions);
    }

    @Test
    void refusesAnEventBelongingToAnotherSpace() {
        UUID eventId = UUID.randomUUID();
        when(events.findById(eventId)).thenReturn(Optional.of(event(eventId, UUID.randomUUID(), null, null)));

        assertThatThrownBy(() -> handler.delete(eventId, caller))
            .isInstanceOf(CalendarException.EventNotFound.class);
        verify(events, never()).delete(any());
    }

    @Test
    void refusesAnEventThatDoesNotExist() {
        UUID eventId = UUID.randomUUID();
        when(events.findById(eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.delete(eventId, caller))
            .isInstanceOf(CalendarException.EventNotFound.class);
    }

    private CalendarEvent event(UUID id, UUID space, UUID seriesId, LocalDate originalDate) {
        return new CalendarEvent(
            id, space, "Piano", null, null, true,
            LocalDate.of(2026, 2, 5), null, LocalDate.of(2026, 2, 5), null,
            null, List.of(), seriesId, originalDate, UUID.randomUUID(), Instant.now());
    }
}
