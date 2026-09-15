package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.DeleteEventUseCase;
import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.calendar.domain.port.out.EventExclusionRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class DeleteEventHandler implements DeleteEventUseCase {

    private final CalendarEventRepository events;
    private final EventExclusionRepository exclusions;

    public DeleteEventHandler(CalendarEventRepository events, EventExclusionRepository exclusions) {
        this.events = events;
        this.exclusions = exclusions;
    }

    @Override
    @Transactional
    public void delete(UUID eventId, SpaceMembership caller) {
        CalendarEvent event = events.findById(eventId).orElseThrow(CalendarException.EventNotFound::new);
        if (!event.spaceId().equals(caller.spaceId())) {
            // Not a 403: revealing that an id exists in some other context is itself a leak.
            throw new CalendarException.EventNotFound();
        }
        events.delete(eventId);
        // A detached occurrence is a row that *replaces* a slot of its series. Deleting the row
        // without excluding the slot hands the slot straight back to the projector, and the
        // occurrence the user just deleted reappears at its original date.
        if (event.isDetachedOccurrence()) {
            exclusions.exclude(event.recurringSeriesId(), event.recurringOriginalDate());
        }
    }
}
