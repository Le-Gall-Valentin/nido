package com.nido.api.mfa.infrastructure.identity;

import com.nido.api.identity.application.port.in.FindUserUseCase;
import com.nido.api.identity.domain.model.User;
import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountAddressAdapterTest {

    private final FindUserUseCase findUser = mock(FindUserUseCase.class);
    private final AccountAddressAdapter adapter = new AccountAddressAdapter(findUser);
    private final UUID jane = UUID.randomUUID();

    @Test
    void the_address_is_the_one_identity_keeps_now() {
        when(findUser.findById(jane)).thenReturn(Optional.of(
            new User(jane, "jane", "jane@example.fr", Role.USER, true, Instant.now(), null)));

        assertThat(adapter.addressOf(jane)).contains("jane@example.fr");
    }

    @Test
    void an_unknown_or_anonymised_account_has_none() {
        UUID ghost = UUID.randomUUID();
        when(findUser.findById(jane)).thenReturn(Optional.empty());
        when(findUser.findById(ghost)).thenReturn(Optional.of(
            new User(ghost, null, null, Role.USER, false, Instant.now(), null)));

        assertThat(adapter.addressOf(jane)).isEmpty();
        assertThat(adapter.addressOf(ghost)).isEmpty();
    }
}
