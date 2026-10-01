package com.nido.api.space.domain.model;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

public record InviteMemberCommand(UUID spaceId, String identifier, SpaceRole role, UUID invitedBy) {

    public static final Duration VALIDITY = Duration.ofDays(7);

    public InviteMemberCommand {
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(invitedBy, "invitedBy");
        if (role == SpaceRole.OWNER) {
            throw new SpaceException.OwnerRoleNotAssignable();
        }
    }
}
