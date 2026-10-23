package com.nido.api.calendar.infrastructure.security;

import com.nido.api.calendar.domain.model.SubscriptionTokens;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NidoSubscriptionTokenGeneratorTest {

    private final NidoSubscriptionTokenGenerator generator = new NidoSubscriptionTokenGenerator();

    @Test
    void aTokenIs32RandomBytesInBase64UrlOfTheShapeLookedUp() {
        String token = generator.generate();

        assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
        assertThat(SubscriptionTokens.wellFormed(token)).isTrue();
        assertThat(generator.generate()).isNotEqualTo(token);
    }
}
