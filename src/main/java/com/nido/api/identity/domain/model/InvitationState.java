package com.nido.api.identity.domain.model;

import java.time.Instant;

/** The invitation of an account that has not chosen its password yet: still usable, or expired. */
public record InvitationState(boolean expired, Instant expiresAt) {
}
