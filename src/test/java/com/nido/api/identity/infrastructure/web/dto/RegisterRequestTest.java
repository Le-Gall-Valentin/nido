package com.nido.api.identity.infrastructure.web.dto;

import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestTest {

    @Test
    void its_string_form_never_shows_the_address_or_the_password() {
        String shown = new RegisterRequest("jane", "jane@test.com", "S3cr3t!Pass", Role.USER).toString();

        assertThat(shown).doesNotContain("jane@test.com").doesNotContain("S3cr3t!Pass").contains("jane");
    }
}
