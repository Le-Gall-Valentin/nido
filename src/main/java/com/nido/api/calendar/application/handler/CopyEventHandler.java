package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.service.CalendarSpaceMemberValidator;
import com.nido.api.calendar.application.port.in.CopyEventUseCase;
import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CalendarException;
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
    private final CalendarSpaceMemberValidator memberValidator;

    public CopyEventHandler(CalendarEventRepository events, ResolveMembershipUseCase resolveMembershipUseCase,
                            CalendarSpaceMemberValidator memberValidator) {
        this.events = events;
        this.resolveMembershipUseCase = resolveMembershipUseCase;
        this.memberValidator = memberValidator;
    }

    @Override
    @Transactional
    public CalendarEvent copy(UUID eventId, UUID destinationSpaceId, SpaceMembership caller) {
        CalendarEvent source = EventTransfer.readInCallersSpace(events, eventId, caller);
        if (destinationSpaceId.equals(caller.spaceId())) {
            throw new CalendarException.SameSpaceTransfer();
        }
        SpaceMembership destination = resolveMembershipUseCase.resolve(destinationSpaceId, caller.userId());
        destination.ensureCanWrite();
        return events.create(EventTransfer.arrivingIn(source, destinationSpaceId, caller.userId())
            .withParticipants(memberValidator.participantsFor(destination, List.of())));
    }
}
