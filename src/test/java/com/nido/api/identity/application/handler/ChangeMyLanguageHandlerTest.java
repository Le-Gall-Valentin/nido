package com.nido.api.identity.application.handler;

import com.nido.api.identity.domain.model.IdentityException;
import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.domain.port.out.UserCommandPort;
import com.nido.api.identity.domain.port.out.UserRepository;
import com.nido.api.shared.model.Language;
import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeMyLanguageHandlerTest {

    @Mock UserRepository userRepository;
    @Mock UserCommandPort userCommandPort;

    private final UUID userId = UUID.randomUUID();

    @Test
    void records_the_language_on_the_account() {
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(new User(userId, "jane", "jane@test.com", Role.USER, true, Instant.now())));

        new ChangeMyLanguageHandler(userRepository, userCommandPort).changeLanguage(userId, Language.EN);

        verify(userCommandPort).updateLanguage(userId, Language.EN);
    }

    @Test
    void a_deactivated_account_changes_nothing() {
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(new User(userId, "jane", "jane@test.com", Role.USER, false, Instant.now())));

        assertThatThrownBy(() -> new ChangeMyLanguageHandler(userRepository, userCommandPort).changeLanguage(userId, Language.FR))
            .isInstanceOf(IdentityException.UserNotActive.class);
        verify(userCommandPort, never()).updateLanguage(any(), any());
    }
}
