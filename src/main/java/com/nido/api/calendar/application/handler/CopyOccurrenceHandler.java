package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.CopyOccurrenceUseCase;
import com.nido.api.calendar.application.service.CalendarSpaceMemberValidator;
import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.CreateEventCommand;
import com.nido.api.calendar.domain.model.EventRecurrenceProjector;
import com.nido.api.calendar.domain.model.EventScheduleValidator;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.calendar.domain.port.out.EventExclusionRepository;
import com.nido.api.calendar.domain.port.out.RecurringEventSeriesRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.ResolveMembershipUseCase;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Copying one occurrence of a series into another space, as an event of its own there — the same rules
 * as copying an event: write access in the destination only, and nothing local to the source travels.
 *
 * <p>What travels is the occurrence as it is shown: the event it was edited into, if it was, or else the
 * series on that day. A slot the series does not produce, or has cancelled, is not an occurrence.
 */
@ApplicationService
public class CopyOccurrenceHandler implements CopyOccurrenceUseCase {

    private final RecurringEventSeriesRepository series;
    private final CalendarEventRepository events;
    private final EventExclusionRepository exclusions;
    private final ResolveMembershipUseCase resolveMembershipUseCase;
    private final CalendarSpaceMemberValidator memberValidator;

    public CopyOccurrenceHandler(RecurringEventSeriesRepository series, CalendarEventRepository events,
                                 EventExclusionRepository exclusions, ResolveMembershipUseCase resolveMembershipUseCase,
                                 CalendarSpaceMemberValidator memberValidator) {
        this.series = series;
        this.events = events;
        this.exclusions = exclusions;
        this.resolveMembershipUseCase = resolveMembershipUseCase;
        this.memberValidator = memberValidator;
    }

    @Override
    @Transactional
    public CalendarEvent copy(UUID seriesId, LocalDate originalDate, UUID destinationSpaceId, SpaceMembership caller) {
        RecurringEventSeries found = series.findById(seriesId)
            .orElseThrow(CalendarException.RecurringEventSeriesNotFound::new);
        if (!found.spaceId().equals(caller.spaceId())) {
            throw new CalendarException.RecurringEventSeriesNotFound();
        }
        if (!EventRecurrenceProjector.slotsBetween(found, originalDate, originalDate).contains(originalDate)
                || exclusions.findAllSlots(seriesId).contains(originalDate)) {
            throw new CalendarException.OccurrenceNotInSeries();
        }
        if (destinationSpaceId.equals(caller.spaceId())) {
            throw new CalendarException.SameSpaceTransfer();
        }
        SpaceMembership destination = resolveMembershipUseCase.resolve(destinationSpaceId, caller.userId());
        destination.ensureCanWrite();
        CreateEventCommand arriving = events.findBySeriesAndOriginalDate(seriesId, originalDate)
            .map(edited -> EventTransfer.arrivingIn(edited, destinationSpaceId, caller.userId()))
            .orElseGet(() -> EventTransfer.occurrenceArrivingIn(found, originalDate, destinationSpaceId, caller.userId()));
        // An occurrence may last as long as its interval, which a single event may not.
        EventScheduleValidator.validateEvent(arriving.allDay(), arriving.startDate(), arriving.startTime(),
            arriving.endDate(), arriving.endTime());
        return events.create(arriving.withParticipants(memberValidator.participantsFor(destination, List.of())));
    }
}
