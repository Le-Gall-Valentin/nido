package com.nido.api.instance.domain.model;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SetupCodeTest {

    @RepeatedTest(20)
    void a_code_is_three_groups_of_four_unambiguous_characters() {
        assertThat(SetupCode.generate().value()).matches("[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}");
    }

    @Test
    void it_is_accepted_however_it_was_typed_from_a_terminal() {
        SetupCode code = new SetupCode("K7QM-3XRP-W9TD");

        assertThat(code.matches("K7QM-3XRP-W9TD")).isTrue();
        assertThat(code.matches("k7qm-3xrp-w9td")).isTrue();
        assertThat(code.matches(" K7QM 3XRP W9TD ")).isTrue();
        assertThat(code.matches("K7QM3XRPW9TD")).isTrue();
        assertThat(code.matches("K7QM-3XRP-W9TE")).isFalse();
        assertThat(code.matches("")).isFalse();
        assertThat(code.matches(null)).isFalse();
    }

    @Test
    void it_never_shows_in_a_log_line_by_accident() {
        assertThat(new SetupCode("K7QM-3XRP-W9TD").toString()).doesNotContain("K7QM");
    }
}
