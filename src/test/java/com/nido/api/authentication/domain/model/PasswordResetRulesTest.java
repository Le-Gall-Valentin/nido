package com.nido.api.authentication.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetRulesTest {

    private final Instant now = Instant.parse("2026-09-28T10:00:00Z");

    @Test
    void a_link_sent_less_than_five_minutes_ago_holds_the_next_one_back() {
        assertThat(PasswordResetRules.inCooldown(now.minus(Duration.ofMinutes(4)), now)).isTrue();
        assertThat(PasswordResetRules.inCooldown(now.minus(Duration.ofMinutes(5)), now)).isFalse();
    }

    @Test
    void a_link_lives_thirty_minutes() {
        PasswordResetToken token = new PasswordResetToken(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(),
            now, now.plus(PasswordResetRules.VALIDITY));

        assertThat(PasswordResetRules.VALIDITY).isEqualTo(Duration.ofMinutes(30));
        assertThat(token.isExpiredAt(now.plus(Duration.ofMinutes(29)))).isFalse();
        assertThat(token.isExpiredAt(now.plus(Duration.ofMinutes(30)))).isTrue();
    }
}
