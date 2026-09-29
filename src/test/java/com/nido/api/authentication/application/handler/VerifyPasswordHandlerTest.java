package com.nido.api.authentication.application.handler;

import com.nido.api.authentication.domain.model.UserCredentials;
import com.nido.api.authentication.domain.port.out.PasswordVerifierPort;
import com.nido.api.authentication.domain.port.out.UserCredentialsPort;
import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerifyPasswordHandlerTest {

    @Mock UserCredentialsPort credentials;
    @Mock PasswordVerifierPort verifier;

    private final UUID userId = UUID.randomUUID();

    @Test
    void answers_whether_the_password_is_the_accounts() {
        when(credentials.findById(userId)).thenReturn(Optional.of(
            new UserCredentials(userId, "jane", "jane@test.com", "$hash", true, Role.USER, Instant.now())));
        when(verifier.matches("right", "$hash")).thenReturn(true);
        when(verifier.matches("wrong", "$hash")).thenReturn(false);

        VerifyPasswordHandler handler = new VerifyPasswordHandler(credentials, verifier);

        assertThat(handler.verify(userId, "right")).isTrue();
        assertThat(handler.verify(userId, "wrong")).isFalse();
    }

    @Test
    void an_unknown_account_has_no_right_password() {
        when(credentials.findById(userId)).thenReturn(Optional.empty());

        assertThat(new VerifyPasswordHandler(credentials, verifier).verify(userId, "anything")).isFalse();
    }
}
