package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.UpdateRecurringEventSeriesUseCase;
import com.nido.api.calendar.application.service.CalendarSpaceMemberValidator;
import com.nido.api.calendar.application.service.SeriesExceptionsMover;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.CreateRecurringEventSeriesCommand;
import com.nido.api.calendar.domain.model.EventRecurrenceProjector;
import com.nido.api.calendar.domain.model.EventScheduleValidator;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.model.UpdateRecurringEventSeriesCommand;
import com.nido.api.calendar.domain.port.out.RecurringEventSeriesRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Editing a whole series never rewrites its past. A series that has begun and is not over stops the
 * day before the edit, keeping every occurrence before it as it was; the edit carries on from that day
 * in a series of its own, on the rhythm it was given. A series not begun yet, or already over, lies on
 * one side of today only, and changes whole.
 *
 * <p>Either way, what was edited or cancelled on its own stays as chosen: see {@link SeriesExceptionsMover}.
 * Before this, moving a series' start date left those exceptions on the old dates, and a week showed the
 * edited occurrence next to the new schedule's.
 */
@ApplicationService
public class UpdateRecurringEventSeriesHandler implements UpdateRecurringEventSeriesUseCase {

    private final RecurringEventSeriesRepository series;
    private final CalendarSpaceMemberValidator memberValidator;
    private final GetSpaceTodayUseCase spaceToday;
    private final SeriesExceptionsMover exceptions;

    public UpdateRecurringEventSeriesHandler(RecurringEventSeriesRepository series,
                                             CalendarSpaceMemberValidator memberValidator,
                                             GetSpaceTodayUseCase spaceToday, SeriesExceptionsMover exceptions) {
        this.series = series;
        this.memberValidator = memberValidator;
        this.spaceToday = spaceToday;
        this.exceptions = exceptions;
    }

    @Override
    @Transactional
    public RecurringEventSeries update(UpdateRecurringEventSeriesCommand command, SpaceMembership caller) {
        caller.ensureCanWrite();
        RecurringEventSeries existing = series.findById(command.seriesId())
            .orElseThrow(CalendarException.RecurringEventSeriesNotFound::new);
        if (!existing.spaceId().equals(caller.spaceId())) {
            throw new CalendarException.RecurringEventSeriesNotFound();
        }
        EventScheduleValidator.validateSeries(command.allDay(), command.startTime(), command.endTime(),
            command.durationDays(), command.intervalType(), command.intervalCount(), command.anchorDate(), command.endDate());
        UpdateRecurringEventSeriesCommand stored =
            command.withParticipants(memberValidator.participantsFor(caller, command.participantIds(), existing.participantIds()));

        LocalDate today = spaceToday.today(caller.spaceId());
        if (!EventRecurrenceProjector.runsAcross(existing, today)) {
            RecurringEventSeries updated = series.update(stored);
            exceptions.carryOver(existing.id(), null, updated);
            return updated;
        }

        series.endOn(existing.id(), today.minusDays(1));
        if (stored.endDate() != null && stored.endDate().isBefore(today)) {
            // Ended before today: the edit leaves nothing to carry on.
            exceptions.letGo(existing.id(), today);
            return series.findById(existing.id()).orElseThrow(CalendarException.RecurringEventSeriesNotFound::new);
        }
        RecurringEventSeries carriedOn = series.create(new CreateRecurringEventSeriesCommand(existing.spaceId(),
            stored.title(), stored.description(), stored.location(), stored.allDay(), stored.startTime(), stored.endTime(),
            stored.durationDays(), stored.color(), stored.intervalType(), stored.intervalCount(), stored.anchorDate(),
            stored.endDate(), stored.participantIds(), existing.createdBy(),
            // Shown from today on, whatever the anchor: the days before it belong to the series as it was.
            stored.anchorDate().isBefore(today) ? today : null));
        exceptions.carryOver(existing.id(), today, carriedOn);
        return carriedOn;
    }
}
