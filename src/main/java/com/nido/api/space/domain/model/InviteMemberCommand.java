package com.nido.api.space.domain.model;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record InviteMemberCommand(UUID spaceId, String email, SpaceRole role, UUID invitedBy) {

    public static final Duration VALIDITY = Duration.ofDays(7);

    public InviteMemberCommand {
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(invitedBy, "invitedBy");
        email = email == null ? null : email.toLowerCase(Locale.ROOT);
        if (role == SpaceRole.OWNER) {
            throw new SpaceException.OwnerRoleNotAssignable();
        }
    }
}
