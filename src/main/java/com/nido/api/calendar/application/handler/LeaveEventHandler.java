package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.service.CalendarSpaceMemberValidator;
import com.nido.api.calendar.application.port.in.LeaveEventUseCase;
import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Removing yourself from an event. Idempotent, like joining. */
@ApplicationService
public class LeaveEventHandler implements LeaveEventUseCase {

    private final CalendarEventRepository events;
    private final CalendarSpaceMemberValidator memberValidator;

    public LeaveEventHandler(CalendarEventRepository events, CalendarSpaceMemberValidator memberValidator) {
        this.events = events;
        this.memberValidator = memberValidator;
    }

    @Override
    @Transactional
    public void leave(UUID eventId, SpaceMembership caller) {
        CalendarEvent event = events.findById(eventId).orElseThrow(CalendarException.EventNotFound::new);
        if (!event.spaceId().equals(caller.spaceId())) {
            throw new CalendarException.EventNotFound();
        }
        // In a personal space its owner always takes part: nothing to join, nothing to leave.
        memberValidator.ensureParticipationOpen(caller);
        events.removeParticipant(eventId, caller.userId());
    }
}
