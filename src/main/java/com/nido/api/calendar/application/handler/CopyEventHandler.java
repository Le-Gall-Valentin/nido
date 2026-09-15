package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.CopyEventUseCase;
import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.CreateEventCommand;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.ResolveMembershipUseCase;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Copying needs write access in the <em>destination</em> only: being able to read an item is
 * enough to justify reproducing it elsewhere, so a VIEWER can copy into any context where they
 * themselves may write.
 *
 * <p>What travels is the event's identity — title, description, location, schedule, colour. What
 * does not is everything local to the source context: its participants, who are members of a
 * space the destination knows nothing about, and its series link, which would dangle because the
 * destination has no such series. Same convention as a copied recipe resetting {@code favorite}.
 */
@ApplicationService
public class CopyEventHandler implements CopyEventUseCase {

    private final CalendarEventRepository events;
    private final ResolveMembershipUseCase resolveMembershipUseCase;

    public CopyEventHandler(CalendarEventRepository events, ResolveMembershipUseCase resolveMembershipUseCase) {
        this.events = events;
        this.resolveMembershipUseCase = resolveMembershipUseCase;
    }

    @Override
    @Transactional
    public CalendarEvent copy(UUID eventId, UUID destinationSpaceId, SpaceMembership caller) {
        CalendarEvent source = readInCallersSpace(events, eventId, caller);
        if (destinationSpaceId.equals(caller.spaceId())) {
            throw new CalendarException.SameSpaceTransfer();
        }
        SpaceMembership destination = resolveMembershipUseCase.resolve(destinationSpaceId, caller.userId());
        destination.ensureCanWrite();
        return events.create(commandFor(source, destinationSpaceId, caller.userId()));
    }

    static CalendarEvent readInCallersSpace(CalendarEventRepository events, UUID eventId, SpaceMembership caller) {
        CalendarEvent source = events.findById(eventId).orElseThrow(CalendarException.EventNotFound::new);
        if (!source.spaceId().equals(caller.spaceId())) {
            throw new CalendarException.EventNotFound();
        }
        return source;
    }

    static CreateEventCommand commandFor(CalendarEvent source, UUID destinationSpaceId, UUID createdBy) {
        return new CreateEventCommand(
            destinationSpaceId, source.title(), source.description(), source.location(), source.allDay(),
            source.startDate(), source.startTime(), source.endDate(), source.endTime(), source.color(),
            List.of(), null, null, createdBy);
    }
}
