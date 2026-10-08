package com.nido.api.mfa.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * A code sent by mail, as kept: never the code, never what it is bound to — only their digests.
 *
 * @param bindingHash SHA-256 of what the code is bound to: tells a resend for the same thing from a new request
 * @param codeHash    HMAC-SHA-256 of the code, keyed by what it is bound to
 */
public record SentMailCode(UUID userId, CodePurpose purpose, String bindingHash, String codeHash,
                           int failedAttempts, Instant sentAt, Instant expiresAt) {}
