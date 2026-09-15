package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.DeleteEventUseCase;
import com.nido.api.calendar.application.port.in.MoveEventUseCase;
import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.ResolveMembershipUseCase;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Moving needs write access on <em>both</em> sides, since it removes the event from the source.
 * A VIEWER can therefore never move, whatever their role at the destination.
 *
 * <p>Implemented as create-then-delete rather than an in-place space_id update, matching the
 * convention set by recipes and tasks. The delete goes through {@link DeleteEventUseCase} rather
 * than straight to the repository, so moving a detached occurrence still writes the exclusion
 * that keeps its slot from re-emitting — that rule lives in one place and must not be bypassed.
 */
@ApplicationService
public class MoveEventHandler implements MoveEventUseCase {

    private final CalendarEventRepository events;
    private final ResolveMembershipUseCase resolveMembershipUseCase;
    private final DeleteEventUseCase deleteEventUseCase;

    public MoveEventHandler(CalendarEventRepository events, ResolveMembershipUseCase resolveMembershipUseCase,
                            DeleteEventUseCase deleteEventUseCase) {
        this.events = events;
        this.resolveMembershipUseCase = resolveMembershipUseCase;
        this.deleteEventUseCase = deleteEventUseCase;
    }

    @Override
    @Transactional
    public CalendarEvent move(UUID eventId, UUID destinationSpaceId, SpaceMembership caller) {
        caller.ensureCanWrite();
        CalendarEvent source = CopyEventHandler.readInCallersSpace(events, eventId, caller);
        if (destinationSpaceId.equals(caller.spaceId())) {
            throw new CalendarException.SameSpaceTransfer();
        }
        SpaceMembership destination = resolveMembershipUseCase.resolve(destinationSpaceId, caller.userId());
        destination.ensureCanWrite();
        CalendarEvent created = events.create(
            CopyEventHandler.commandFor(source, destinationSpaceId, caller.userId()));
        deleteEventUseCase.delete(eventId, caller);
        return created;
    }
}
