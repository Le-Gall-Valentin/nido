package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.UpdateEventUseCase;
import com.nido.api.calendar.application.service.CalendarSpaceMemberValidator;
import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.EventScheduleValidator;
import com.nido.api.calendar.domain.model.UpdateEventCommand;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class UpdateEventHandler implements UpdateEventUseCase {

    private final CalendarEventRepository events;
    private final CalendarSpaceMemberValidator memberValidator;

    public UpdateEventHandler(CalendarEventRepository events, CalendarSpaceMemberValidator memberValidator) {
        this.events = events;
        this.memberValidator = memberValidator;
    }

    @Override
    @Transactional
    public CalendarEvent update(UpdateEventCommand command, SpaceMembership caller) {
        caller.ensureCanWrite();
        CalendarEvent existing = events.findById(command.eventId())
            .orElseThrow(CalendarException.EventNotFound::new);
        if (!existing.spaceId().equals(caller.spaceId())) {
            throw new CalendarException.EventNotFound();
        }
        EventScheduleValidator.validateEvent(
            command.allDay(), command.startDate(), command.startTime(), command.endDate(), command.endTime());
        return events.update(command.withParticipants(
            memberValidator.participantsFor(caller, command.participantIds(), existing.participantIds())));
    }
}
