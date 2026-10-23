package com.nido.api.calendar.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionTokensTest {

    @Test
    void onlyATokenOfThatShapeIsLookedUp() {
        assertThat(SubscriptionTokens.wellFormed("aB3-_".repeat(8) + "xyz")).isTrue();
        assertThat(SubscriptionTokens.wellFormed("short")).isFalse();
        assertThat(SubscriptionTokens.wellFormed("a".repeat(42) + "/")).isFalse();
        assertThat(SubscriptionTokens.wellFormed(null)).isFalse();
    }
}
