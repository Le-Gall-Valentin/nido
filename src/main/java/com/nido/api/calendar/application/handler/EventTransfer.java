package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.CreateEventCommand;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.space.domain.model.SpaceMembership;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** What copying and moving share: finding the event in the caller's space, and what of it travels. */
final class EventTransfer {

    private EventTransfer() {}

    /** The event, if it lies in the caller's space; a 404 otherwise, never a hint that it exists elsewhere. */
    static CalendarEvent readInCallersSpace(CalendarEventRepository events, UUID eventId, SpaceMembership caller) {
        CalendarEvent source = events.findById(eventId).orElseThrow(CalendarException.EventNotFound::new);
        if (!source.spaceId().equals(caller.spaceId())) {
            throw new CalendarException.EventNotFound();
        }
        return source;
    }

    /**
     * The event as it arrives in the destination: its identity and schedule, without what is local to
     * the space it leaves — its participants, and its series link.
     */
    static CreateEventCommand arrivingIn(CalendarEvent source, UUID destinationSpaceId, UUID createdBy) {
        return new CreateEventCommand(
            destinationSpaceId, source.title(), source.description(), source.location(), source.allDay(),
            source.startDate(), source.startTime(), source.endDate(), source.endTime(), source.color(),
            List.of(), null, null, createdBy);
    }

    /** One occurrence of a series as it arrives in the destination: the series on that day, as an event. */
    static CreateEventCommand occurrenceArrivingIn(RecurringEventSeries source, LocalDate slot, UUID destinationSpaceId,
                                                   UUID createdBy) {
        return new CreateEventCommand(
            destinationSpaceId, source.title(), source.description(), source.location(), source.allDay(),
            slot, source.startTime(), slot.plusDays(source.durationDays()), source.endTime(), source.color(),
            List.of(), null, null, createdBy);
    }
}
