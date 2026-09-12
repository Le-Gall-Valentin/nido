package com.nido.api.authentication.infrastructure.security;

import com.nido.api.shared.model.Role;

import java.time.Instant;
import java.util.UUID;

/**
 * @param issuedAt the token's own {@code iat}, kept so a token can be compared against a cut-off
 *                 recorded after it was minted. Every token already carried it — nothing had to be
 *                 added to the format for revocation to become possible.
 */
public record UserClaims(UUID userId, Role role, String email, Instant issuedAt) {}