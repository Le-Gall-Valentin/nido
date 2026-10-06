package com.nido.api.authentication.application.dto;

import java.time.Instant;

/** Where an account's invitation stands: still usable, or expired and waiting to be issued again. */
public record InvitationStatus(boolean expired, Instant expiresAt) {
}
