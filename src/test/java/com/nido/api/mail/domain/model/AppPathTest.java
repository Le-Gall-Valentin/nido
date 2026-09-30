package com.nido.api.mail.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppPathTest {

    @Test
    void a_fragment_is_kept_out_of_the_string_form_but_not_out_of_the_value() {
        AppPath path = new AppPath("/reset-password#token=SECRET");

        assertThat(path.toString()).isEqualTo("/reset-password#…").doesNotContain("SECRET");
        assertThat(path.value()).isEqualTo("/reset-password#token=SECRET");
    }

    @Test
    void a_path_without_a_fragment_prints_as_is() {
        assertThat(new AppPath("/login").toString()).isEqualTo("/login");
    }
}
