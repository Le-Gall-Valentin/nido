package com.nido.api.mail.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RetryPolicyTest {

    private final RetryPolicy policy = new RetryPolicy();
    private final Instant now = Instant.parse("2026-09-28T10:00:00Z");

    @Test
    void waits_longer_after_each_failure() {
        assertThat(policy.nextAttempt(1, now, null)).contains(now.plus(Duration.ofMinutes(1)));
        assertThat(policy.nextAttempt(2, now, null)).contains(now.plus(Duration.ofMinutes(5)));
        assertThat(policy.nextAttempt(3, now, null)).contains(now.plus(Duration.ofMinutes(15)));
        assertThat(policy.nextAttempt(4, now, null)).contains(now.plus(Duration.ofMinutes(60)));
        assertThat(policy.nextAttempt(5, now, null)).contains(now.plus(Duration.ofMinutes(240)));
    }

    @Test
    void gives_up_after_the_sixth_failed_attempt() {
        assertThat(policy.nextAttempt(6, now, null)).isEmpty();
        assertThat(RetryPolicy.MAX_ATTEMPTS).isEqualTo(6);
    }

    @Test
    void never_schedules_an_attempt_at_or_past_the_expiry() {
        // A reset link that dies in 3 minutes must not be retried in 5.
        Instant expiresAt = now.plus(Duration.ofMinutes(3));

        assertThat(policy.nextAttempt(1, now, expiresAt)).contains(now.plus(Duration.ofMinutes(1)));
        assertThat(policy.nextAttempt(2, now, expiresAt)).isEmpty();
        assertThat(policy.nextAttempt(1, now, now.plus(Duration.ofMinutes(1)))).isEmpty();
    }
}
