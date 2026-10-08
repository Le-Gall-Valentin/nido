package com.nido.api.mfa.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * A code sent by mail, as kept: never the code, never what it is bound to — only their keyed digests
 * ({@link com.nido.api.mfa.domain.port.out.MailCodeHasherPort}), which the database alone cannot recompute.
 *
 * @param bindingHash digest of what the code is bound to: tells a resend for the same thing from a new request
 * @param codeHash    digest of the code together with what it is bound to
 */
public record SentMailCode(UUID userId, CodePurpose purpose, String bindingHash, String codeHash,
                           int failedAttempts, Instant sentAt, Instant expiresAt) {}
