package com.nido.api.authentication.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountInvitationTest {

    private final Instant expiresAt = Instant.parse("2026-10-12T10:00:00Z");
    private final AccountInvitation invitation =
        new AccountInvitation(UUID.randomUUID(), Instant.parse("2026-10-05T10:00:00Z"), expiresAt);

    @Test
    void it_expires_at_its_expiry_and_not_a_moment_before() {
        assertThat(invitation.isExpiredAt(expiresAt.minusMillis(1))).isFalse();
        assertThat(invitation.isExpiredAt(expiresAt)).isTrue();
    }

    @Test
    void the_link_lasts_a_week_and_carries_its_token_after_the_hash() {
        assertThat(AccountInvitationRules.VALIDITY).hasDays(7);
        assertThat(AccountInvitationRules.welcomePath("RAW-token_1")).isEqualTo("/welcome#token=RAW-token_1");
    }
}
