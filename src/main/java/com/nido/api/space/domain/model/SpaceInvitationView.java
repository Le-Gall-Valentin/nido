package com.nido.api.space.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Le code figure en clair, c'est voulu : la liste des invitations en cours est réservée
 * aux gestionnaires du groupe, qui sont les personnes qui les ont émises. {@code username} est
 * null quand le compte invité n'est plus résoluble.
 */
public record SpaceInvitationView(
    UUID id,
    String username,
    SpaceRole role,
    String code,
    InvitationStatus status,
    Instant expiresAt,
    Instant createdAt
) {}
