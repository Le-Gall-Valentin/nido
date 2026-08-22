package com.nido.api.space.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;

import java.util.UUID;

public interface ResolveMembershipUseCase {
    /** Lève SpaceException.NotAMember (traduit en 404) si l'utilisateur n'est pas membre. */
    SpaceMembership resolve(UUID spaceId, UUID userId);
}
