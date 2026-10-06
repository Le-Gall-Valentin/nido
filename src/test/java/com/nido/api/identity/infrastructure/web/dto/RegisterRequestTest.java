package com.nido.api.identity.infrastructure.web.dto;

import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestTest {

    @Test
    void its_string_form_never_shows_the_address() {
        String shown = new RegisterRequest("jane", "jane@test.com", Role.USER).toString();

        assertThat(shown).doesNotContain("jane@test.com").contains("jane");
    }
}
