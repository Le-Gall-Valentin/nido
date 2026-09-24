package com.nido.api.calendar.application.service;

import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.GetSpaceUseCase;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Who takes part in what is written into a space's calendar.
 *
 * <p>In a personal space its owner — the one person in it — takes part in everything, whatever a
 * request says, and there is nothing to join or leave. Every write resolves its participants here,
 * so no path (a form, a series, a single occurrence, a copy or a move into the space) can leave an
 * event of a personal space without its owner.
 *
 * <p>Elsewhere, every participant id a caller submitted must be an actual member of the space.
 * Nothing else constrains those ids, so without this check an unknown or foreign UUID reaches the
 * database as a foreign key value and blows up with a raw DataIntegrityViolationException (500)
 * instead of a clean 404.
 *
 * <p>Prefixed with {@code Calendar} for the same reason Tasks prefixes its own: Spring's default
 * bean naming ignores the package, so two validators sharing a simple name collide at startup.
 */
@ApplicationService
public class CalendarSpaceMemberValidator {

    private final SpaceMembershipPort spaceMembershipPort;
    private final GetSpaceUseCase getSpaceUseCase;

    public CalendarSpaceMemberValidator(SpaceMembershipPort spaceMembershipPort, GetSpaceUseCase getSpaceUseCase) {
        this.spaceMembershipPort = spaceMembershipPort;
        this.getSpaceUseCase = getSpaceUseCase;
    }

    /**
     * The participants to store for an event or series written by {@code writer} into their space:
     * its owner alone in a personal space, the members requested — each checked — anywhere else.
     */
    public List<UUID> participantsFor(SpaceMembership writer, List<UUID> requested) {
        if (isPersonal(writer)) {
            return List.of(writer.userId());
        }
        ensureMembers(writer.spaceId(), requested);
        return requested;
    }

    /** Refuses joining or leaving an event of a personal space: its owner always takes part. */
    public void ensureParticipationOpen(SpaceMembership caller) {
        if (isPersonal(caller)) {
            throw new CalendarException.ParticipantsFixedInPersonalSpace();
        }
    }

    public void ensureMembers(UUID spaceId, Collection<UUID> memberIds) {
        for (UUID memberId : memberIds) {
            spaceMembershipPort.find(spaceId, memberId)
                .orElseThrow(CalendarException.MemberNotInSpace::new);
        }
    }

    private boolean isPersonal(SpaceMembership member) {
        return getSpaceUseCase.get(member.spaceId(), member).type() == SpaceType.PERSONAL;
    }
}
