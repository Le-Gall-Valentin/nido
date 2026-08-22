package com.nido.api.space.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Le code figure en clair, c'est voulu : la liste des invitations en cours est réservée
 * aux gestionnaires du groupe, qui sont les personnes qui les ont émises.
 */
public record SpaceInvitationView(
    UUID id,
    String email,
    SpaceRole role,
    String code,
    InvitationStatus status,
    Instant expiresAt,
    Instant createdAt
) {}
