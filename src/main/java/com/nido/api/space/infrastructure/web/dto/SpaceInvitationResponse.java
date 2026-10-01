package com.nido.api.space.infrastructure.web.dto;

import com.nido.api.space.domain.model.InvitationStatus;
import com.nido.api.space.domain.model.SpaceInvitationView;
import com.nido.api.space.domain.model.SpaceRole;

import java.time.Instant;
import java.util.UUID;

/**
 * Le code figure en clair, c'est voulu : cette réponse n'est adressée qu'aux gestionnaires
 * du groupe, qui sont les personnes qui ont émis l'invitation. {@code username} est null quand le
 * compte invité n'est plus résoluble ; l'adresse de l'invité n'est jamais renvoyée.
 */
public record SpaceInvitationResponse(
    UUID id,
    String username,
    SpaceRole role,
    String code,
    InvitationStatus status,
    Instant expiresAt,
    Instant createdAt
) {
    public static SpaceInvitationResponse from(SpaceInvitationView view) {
        return new SpaceInvitationResponse(view.id(), view.username(), view.role(), view.code(),
            view.status(), view.expiresAt(), view.createdAt());
    }
}
