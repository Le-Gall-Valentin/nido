package com.nido.api.space.infrastructure.web.dto;

import com.nido.api.space.domain.model.InvitationStatus;
import com.nido.api.space.domain.model.SpaceInvitationView;
import com.nido.api.space.domain.model.SpaceRole;

import java.time.Instant;
import java.util.UUID;

/**
 * Le code figure en clair, c'est voulu : cette réponse n'est adressée qu'aux gestionnaires
 * du groupe, qui sont les personnes qui ont émis l'invitation.
 */
public record SpaceInvitationResponse(
    UUID id,
    String email,
    SpaceRole role,
    String code,
    InvitationStatus status,
    Instant expiresAt,
    Instant createdAt
) {
    public static SpaceInvitationResponse from(SpaceInvitationView view) {
        return new SpaceInvitationResponse(view.id(), view.email(), view.role(), view.code(),
            view.status(), view.expiresAt(), view.createdAt());
    }
}
