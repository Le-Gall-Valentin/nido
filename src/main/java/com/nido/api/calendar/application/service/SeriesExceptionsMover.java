package com.nido.api.calendar.application.service;

import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.ExceptionRehoming;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.calendar.domain.port.out.EventExclusionRepository;
import com.nido.api.shared.annotation.ApplicationService;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * What was chosen on its own in a series — occurrences edited alone, and cancellations — when the
 * series changes. Each is kept: carried over to the nearest occurrence of the new schedule (see
 * {@link ExceptionRehoming}), or, when nothing carries on, an edited occurrence stays an event of its
 * own. Only a series that stops takes what is still to come with it.
 *
 * <p>A null {@code from} means every exception of the series; otherwise those replacing a slot on or
 * after it — the part of the series an edit made after it began is allowed to change.
 */
@ApplicationService
public class SeriesExceptionsMover {

    private final CalendarEventRepository events;
    private final EventExclusionRepository exclusions;

    public SeriesExceptionsMover(CalendarEventRepository events, EventExclusionRepository exclusions) {
        this.events = events;
        this.exclusions = exclusions;
    }

    /** Onto {@code target} — the same series edited whole, or the one an edit carries on in. */
    public void carryOver(UUID seriesId, LocalDate from, RecurringEventSeries target) {
        List<CalendarEvent> edited = editedFrom(seriesId, from);
        ExceptionRehoming.Plan plan = ExceptionRehoming.plan(target,
            edited.stream().map(CalendarEvent::recurringOriginalDate).toList(), cancelledFrom(seriesId, from));
        // Every edited occurrence is let go first, so that two of them never claim one slot on the way.
        edited.forEach(event -> events.relink(event.id(), null, null));
        exclusions.clearFrom(seriesId, from);
        for (CalendarEvent event : edited) {
            LocalDate slot = plan.editedOccurrences().get(event.recurringOriginalDate());
            if (slot != null) {
                events.relink(event.id(), target.id(), slot);
            }
        }
        plan.cancellations().forEach(slot -> exclusions.exclude(target.id(), slot));
    }

    /** Nothing carries on: each edited occurrence stays an event of its own, and no cancellation remains. */
    public void letGo(UUID seriesId, LocalDate from) {
        editedFrom(seriesId, from).forEach(event -> events.relink(event.id(), null, null));
        exclusions.clearFrom(seriesId, from);
    }

    /** The series stops: what is still to come goes, the occurrences edited on their own included. */
    public void dropFrom(UUID seriesId, LocalDate from) {
        editedFrom(seriesId, from).forEach(event -> events.delete(event.id()));
        exclusions.clearFrom(seriesId, from);
    }

    private List<CalendarEvent> editedFrom(UUID seriesId, LocalDate from) {
        return events.findDetachedOf(seriesId).stream()
            .filter(event -> from == null || !event.recurringOriginalDate().isBefore(from))
            .toList();
    }

    private Set<LocalDate> cancelledFrom(UUID seriesId, LocalDate from) {
        return exclusions.findAllSlots(seriesId).stream()
            .filter(slot -> from == null || !slot.isBefore(from))
            .collect(Collectors.toSet());
    }
}
