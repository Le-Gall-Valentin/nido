package com.synapsys.api.authentication.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    private static RefreshToken token(Instant expiresAt) {
        return new RefreshToken(UUID.randomUUID(), UUID.randomUUID(), "hash",
            expiresAt, false, Instant.now(), null);
    }

    @Test
    void isExpired_withClock_returnsTrueWhenClockIsAfterExpiry() {
        Instant expiry = Instant.parse("2026-01-01T12:00:00Z");
        Clock afterExpiry = Clock.fixed(Instant.parse("2026-01-01T13:00:00Z"), ZoneOffset.UTC);

        assertThat(token(expiry).isExpired(afterExpiry)).isTrue();
    }

    @Test
    void isExpired_withClock_returnsFalseWhenClockIsBeforeExpiry() {
        Instant expiry = Instant.parse("2026-01-01T12:00:00Z");
        Clock beforeExpiry = Clock.fixed(Instant.parse("2026-01-01T11:00:00Z"), ZoneOffset.UTC);

        assertThat(token(expiry).isExpired(beforeExpiry)).isFalse();
    }
}