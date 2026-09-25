package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.service.CalendarSpaceMemberValidator;
import com.nido.api.calendar.application.port.in.JoinEventUseCase;
import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Adding yourself to an event. A convenience, not a permission boundary: a MEMBER can already
 * rewrite the whole participant list through an update. It exists so the UI can offer
 * "Me joindre" without opening the full form, and so the intent is legible in the API.
 */
@ApplicationService
public class JoinEventHandler implements JoinEventUseCase {

    private final CalendarEventRepository events;
    private final CalendarSpaceMemberValidator memberValidator;

    public JoinEventHandler(CalendarEventRepository events, CalendarSpaceMemberValidator memberValidator) {
        this.events = events;
        this.memberValidator = memberValidator;
    }

    @Override
    @Transactional
    public void join(UUID eventId, SpaceMembership caller) {
        caller.ensureCanWrite();
        CalendarEvent event = events.findById(eventId).orElseThrow(CalendarException.EventNotFound::new);
        if (!event.spaceId().equals(caller.spaceId())) {
            throw new CalendarException.EventNotFound();
        }
        // In a personal space its owner always takes part: nothing to join, nothing to leave.
        memberValidator.ensureParticipationOpen(caller);
        // Idempotent: joining twice is not an error, so a double tap is harmless.
        events.addParticipant(eventId, caller.userId());
    }
}
