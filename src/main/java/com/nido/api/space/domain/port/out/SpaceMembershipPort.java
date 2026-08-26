package com.nido.api.space.domain.port.out;

import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpaceMembershipPort {
    Optional<SpaceMembership> find(UUID spaceId, UUID userId);
    List<SpaceMembership> findMemberships(UUID spaceId);
    List<SpaceMembership> findByUser(UUID userId);
    SpaceMembership add(UUID spaceId, UUID userId, SpaceRole role);
    void changeRole(UUID membershipId, SpaceRole newRole);
    void remove(UUID membershipId);
    /** Plus ancien ADMIN, à défaut plus ancien MEMBER, jamais un VIEWER. */
    Optional<SpaceMembership> findSuccessor(UUID spaceId, UUID excludedUserId);
}
