package com.nido.api.calendar.application.service;

import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;

import java.util.Collection;
import java.util.UUID;

/**
 * Confirms every participant id a caller submitted is an actual member of the space. Nothing
 * else constrains those ids, so without this check an unknown or foreign UUID reaches the
 * database as a foreign key value and blows up with a raw DataIntegrityViolationException (500)
 * instead of a clean 404.
 *
 * <p>Prefixed with {@code Calendar} for the same reason Tasks prefixes its own: Spring's default
 * bean naming ignores the package, so two validators sharing a simple name collide at startup.
 */
@ApplicationService
public class CalendarSpaceMemberValidator {

    private final SpaceMembershipPort spaceMembershipPort;

    public CalendarSpaceMemberValidator(SpaceMembershipPort spaceMembershipPort) {
        this.spaceMembershipPort = spaceMembershipPort;
    }

    public void ensureMembers(UUID spaceId, Collection<UUID> memberIds) {
        for (UUID memberId : memberIds) {
            spaceMembershipPort.find(spaceId, memberId)
                .orElseThrow(CalendarException.MemberNotInSpace::new);
        }
    }
}
