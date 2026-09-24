package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.UpdateRecurringEventSeriesUseCase;
import com.nido.api.calendar.application.service.CalendarSpaceMemberValidator;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.EventScheduleValidator;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.model.UpdateRecurringEventSeriesCommand;
import com.nido.api.calendar.domain.port.out.RecurringEventSeriesRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class UpdateRecurringEventSeriesHandler implements UpdateRecurringEventSeriesUseCase {

    private final RecurringEventSeriesRepository series;
    private final CalendarSpaceMemberValidator memberValidator;

    public UpdateRecurringEventSeriesHandler(RecurringEventSeriesRepository series,
                                             CalendarSpaceMemberValidator memberValidator) {
        this.series = series;
        this.memberValidator = memberValidator;
    }

    @Override
    @Transactional
    public RecurringEventSeries update(UpdateRecurringEventSeriesCommand command, SpaceMembership caller) {
        RecurringEventSeries existing = series.findById(command.seriesId())
            .orElseThrow(CalendarException.RecurringEventSeriesNotFound::new);
        if (!existing.spaceId().equals(caller.spaceId())) {
            throw new CalendarException.RecurringEventSeriesNotFound();
        }
        EventScheduleValidator.validateSeries(command.allDay(), command.startTime(), command.endTime(),
            command.durationDays(), command.anchorDate(), command.endDate());
        UpdateRecurringEventSeriesCommand stored =
            command.withParticipants(memberValidator.participantsFor(caller, command.participantIds()));
        // Moving the anchor re-dates every slot, so exclusions and detached instances keyed on the
        // old slots stop matching. They are deliberately left alone rather than migrated: guessing
        // which new slot a cancellation was "really" about would be inventing the user's intent.
        return series.update(stored);
    }
}
