package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.CreateEventUseCase;
import com.nido.api.calendar.application.service.CalendarSpaceMemberValidator;
import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CreateEventCommand;
import com.nido.api.calendar.domain.model.EventScheduleValidator;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class CreateEventHandler implements CreateEventUseCase {

    private final CalendarEventRepository events;
    private final CalendarSpaceMemberValidator memberValidator;

    public CreateEventHandler(CalendarEventRepository events, CalendarSpaceMemberValidator memberValidator) {
        this.events = events;
        this.memberValidator = memberValidator;
    }

    @Override
    @Transactional
    public CalendarEvent create(CreateEventCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        EventScheduleValidator.validateEvent(
            command.allDay(), command.startDate(), command.startTime(), command.endDate(), command.endTime());
        memberValidator.ensureMembers(command.spaceId(), command.participantIds());
        return events.create(command);
    }
}
