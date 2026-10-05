package com.nido.api.instance.infrastructure.identity;

import com.nido.api.identity.domain.model.IdentityException;
import com.nido.api.instance.domain.model.InitialAdmin;
import com.nido.api.instance.domain.model.InstanceException;
import com.nido.api.shared.model.Language;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InitialAdminAdapterTest {

    private static final InitialAdmin JANE = new InitialAdmin("jane", "jane@example.fr", "Str0ng!Password", Language.FR);

    @Test
    void the_first_administrator_is_created_by_identity_in_their_language() {
        UUID id = UUID.randomUUID();
        InitialAdminAdapter adapter = new InitialAdminAdapter((username, email, password, language) -> {
            assertThat(username).isEqualTo("jane");
            assertThat(language).isEqualTo(Language.FR);
            return Optional.of(id);
        });

        assertThat(adapter.create(JANE)).contains(id);
    }

    @Test
    void a_username_identity_refuses_is_a_refusal_of_the_setup_naming_the_field() {
        InitialAdminAdapter adapter = new InitialAdminAdapter((username, email, password, language) -> {
            throw new IdentityException.InvalidUsername();
        });

        assertThatThrownBy(() -> adapter.create(JANE))
            .isInstanceOf(InstanceException.InitialAdminRefused.class)
            .hasMessageContaining("username");
    }
}
