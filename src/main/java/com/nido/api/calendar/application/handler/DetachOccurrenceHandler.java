package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.DetachOccurrenceUseCase;
import com.nido.api.calendar.application.service.CalendarSpaceMemberValidator;
import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.CreateEventCommand;
import com.nido.api.calendar.domain.model.EventRecurrenceProjector;
import com.nido.api.calendar.domain.model.EventScheduleValidator;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.model.UpdateEventCommand;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.calendar.domain.port.out.EventExclusionRepository;
import com.nido.api.calendar.domain.port.out.RecurringEventSeriesRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * "Edit just this one." Writes — or rewrites — the single event that takes over one slot of a
 * series, leaving every other occurrence untouched.
 */
@ApplicationService
public class DetachOccurrenceHandler implements DetachOccurrenceUseCase {

    private final RecurringEventSeriesRepository series;
    private final CalendarEventRepository events;
    private final EventExclusionRepository exclusions;
    private final CalendarSpaceMemberValidator memberValidator;

    public DetachOccurrenceHandler(RecurringEventSeriesRepository series, CalendarEventRepository events,
                                   EventExclusionRepository exclusions, CalendarSpaceMemberValidator memberValidator) {
        this.series = series;
        this.events = events;
        this.exclusions = exclusions;
        this.memberValidator = memberValidator;
    }

    @Override
    @Transactional
    public CalendarEvent detach(UUID seriesId, LocalDate originalDate, UpdateEventCommand content,
                                SpaceMembership caller) {
        caller.ensureCanWrite();
        RecurringEventSeries found = series.findById(seriesId)
            .orElseThrow(CalendarException.RecurringEventSeriesNotFound::new);
        if (!found.spaceId().equals(caller.spaceId())) {
            throw new CalendarException.RecurringEventSeriesNotFound();
        }
        // A date the series never produces would create an event no series ever emits: visible in
        // the calendar, tied to a series, and unreachable again from that series' occurrences.
        if (!EventRecurrenceProjector.slotsBetween(found, originalDate, originalDate).contains(originalDate)) {
            throw new CalendarException.OccurrenceNotInSeries();
        }
        EventScheduleValidator.validateEvent(
            content.allDay(), content.startDate(), content.startTime(), content.endDate(), content.endTime());
        Optional<CalendarEvent> existing = events.findBySeriesAndOriginalDate(seriesId, originalDate);
        // Whoever takes part in the series, or in this occurrence as edited before, stays on record.
        Set<UUID> alreadyTakingPart = new HashSet<>(found.participantIds());
        existing.ifPresent(edited -> alreadyTakingPart.addAll(edited.participantIds()));
        List<UUID> participants = memberValidator.participantsFor(caller, content.participantIds(), alreadyTakingPart);

        // Unconditional, and it matters: cancelling an occurrence and then editing it instead
        // would otherwise leave the new event hidden behind the exclusion that is still standing.
        exclusions.clear(seriesId, originalDate);

        if (existing.isPresent()) {
            return events.update(new UpdateEventCommand(
                existing.get().id(), content.title(), content.description(), content.location(), content.allDay(),
                content.startDate(), content.startTime(), content.endDate(), content.endTime(),
                content.color(), participants));
        }
        return events.create(new CreateEventCommand(
            found.spaceId(), content.title(), content.description(), content.location(), content.allDay(),
            content.startDate(), content.startTime(), content.endDate(), content.endTime(),
            content.color(), participants, seriesId, originalDate, caller.userId()));
    }
}
