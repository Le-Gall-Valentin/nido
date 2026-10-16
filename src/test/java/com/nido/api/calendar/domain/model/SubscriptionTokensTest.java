package com.nido.api.calendar.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionTokensTest {

    @Test
    void aTokenIs32RandomBytesInBase64Url() {
        String token = SubscriptionTokens.newToken();
        assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
        assertThat(SubscriptionTokens.newToken()).isNotEqualTo(token);
    }

    @Test
    void onlyATokenOfThatShapeIsLookedUp() {
        assertThat(SubscriptionTokens.wellFormed(SubscriptionTokens.newToken())).isTrue();
        assertThat(SubscriptionTokens.wellFormed("short")).isFalse();
        assertThat(SubscriptionTokens.wellFormed("a".repeat(42) + "/")).isFalse();
        assertThat(SubscriptionTokens.wellFormed(null)).isFalse();
    }
}
