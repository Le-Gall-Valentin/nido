package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.CreateRecurringEventSeriesUseCase;
import com.nido.api.calendar.application.service.CalendarSpaceMemberValidator;
import com.nido.api.calendar.domain.model.CreateRecurringEventSeriesCommand;
import com.nido.api.calendar.domain.model.EventScheduleValidator;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.port.out.RecurringEventSeriesRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class CreateRecurringEventSeriesHandler implements CreateRecurringEventSeriesUseCase {

    private final RecurringEventSeriesRepository series;
    private final CalendarSpaceMemberValidator memberValidator;

    public CreateRecurringEventSeriesHandler(RecurringEventSeriesRepository series,
                                             CalendarSpaceMemberValidator memberValidator) {
        this.series = series;
        this.memberValidator = memberValidator;
    }

    @Override
    @Transactional
    public RecurringEventSeries create(CreateRecurringEventSeriesCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        // No backlog check, unlike Tasks and Finance: nothing is materialized here, so an anchor
        // far in the past costs a projection over the requested window and nothing else.
        EventScheduleValidator.validateSeries(command.allDay(), command.startTime(), command.endTime(),
            command.durationDays(), command.intervalType(), command.intervalCount(), command.anchorDate(), command.endDate());
        return series.create(command.withParticipants(memberValidator.participantsFor(caller, command.participantIds())));
    }
}
