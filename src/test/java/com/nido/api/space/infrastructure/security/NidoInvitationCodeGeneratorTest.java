package com.nido.api.space.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NidoInvitationCodeGeneratorTest {

    private final NidoInvitationCodeGenerator generator = new NidoInvitationCodeGenerator();

    @Test
    void generates_the_documented_format() {
        assertThat(generator.generate()).matches("NIDO-[ABCDEFGHJKLMNPQRSTUVWXYZ0-9]{6}");
    }

    @Test
    void avoids_the_characters_that_read_ambiguously() {
        for (int i = 0; i < 200; i++) {
            assertThat(generator.generate().substring(5)).doesNotContain("I").doesNotContain("O");
        }
    }

    @Test
    void does_not_repeat_itself_over_a_thousand_draws() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            seen.add(generator.generate());
        }
        // 34^6 possibilités : une collision sur mille tirages signalerait un générateur cassé.
        assertThat(seen).hasSize(1000);
    }
}
