package com.nido.api.authentication.domain.model;

import java.time.Duration;
import java.time.Instant;

/**
 * How long a reset link lives, and how often one may be sent.
 *
 * <p>The cooldown is per account, whatever the IP: without it, anyone who knows an address could
 * flood that mailbox from as many IPs as they have. Within it, the previous link stays valid and
 * nothing new is sent — the page already tells people to look in their spam and try again later.
 */
public final class PasswordResetRules {

    public static final Duration VALIDITY = Duration.ofMinutes(30);
    public static final Duration COOLDOWN = Duration.ofMinutes(5);

    private PasswordResetRules() {}

    public static boolean inCooldown(Instant lastIssuedAt, Instant now) {
        return now.isBefore(lastIssuedAt.plus(COOLDOWN));
    }
}
