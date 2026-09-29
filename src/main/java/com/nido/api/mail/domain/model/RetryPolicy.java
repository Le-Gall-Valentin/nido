package com.nido.api.mail.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * When to try a mail again after a temporary failure: 1, 5, 15, 60 and 240 minutes after each failed
 * attempt — six attempts over a little more than five hours — and never at or past the mail's expiry.
 */
public final class RetryPolicy {

    private static final List<Duration> DELAYS = List.of(
        Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(15),
        Duration.ofMinutes(60), Duration.ofMinutes(240));

    public static final int MAX_ATTEMPTS = DELAYS.size() + 1;

    /**
     * @param failedAttempts attempts that have failed, the one that just failed included (1 or more)
     * @return when to try next, or empty to give up
     */
    public Optional<Instant> nextAttempt(int failedAttempts, Instant now, Instant expiresAt) {
        if (failedAttempts < 1 || failedAttempts > DELAYS.size()) {
            return Optional.empty();
        }
        Instant next = now.plus(DELAYS.get(failedAttempts - 1));
        if (expiresAt != null && !next.isBefore(expiresAt)) {
            return Optional.empty();
        }
        return Optional.of(next);
    }
}
