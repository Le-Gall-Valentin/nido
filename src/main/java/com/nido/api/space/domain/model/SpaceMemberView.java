package com.nido.api.space.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code username} et {@code email} sont null quand le profil du compte n'est plus
 * résoluble, ce qui est le cas d'un compte anonymisé par une suppression RGPD.
 */
public record SpaceMemberView(
    UUID userId,
    String username,
    String email,
    SpaceRole role,
    Instant joinedAt
) {}
