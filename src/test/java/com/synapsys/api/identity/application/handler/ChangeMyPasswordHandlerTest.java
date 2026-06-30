package com.synapsys.api.identity.application.handler;

import com.synapsys.api.identity.domain.model.ChangeMyPasswordCommand;
import com.synapsys.api.identity.domain.model.IdentityException;
import com.synapsys.api.identity.domain.model.User;
import com.synapsys.api.identity.domain.port.out.CredentialChangePort;
import com.synapsys.api.identity.domain.port.out.UserRepository;
import com.synapsys.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangeMyPasswordHandlerTest {

    @Mock UserRepository userRepository;
    @Mock CredentialChangePort credentialChangePort;

    private ChangeMyPasswordHandler handler;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ChangeMyPasswordHandler(userRepository, credentialChangePort);
    }

    @Test
    void changeMyPassword_activeUser_delegatesToCredentialChangePort() {
        User user = new User(userId, "u", "u@test.com", Role.USER, true, Instant.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        ChangeMyPasswordCommand cmd = new ChangeMyPasswordCommand(userId, "old", "Newpass1!");
        assertThatCode(() -> handler.changeMyPassword(cmd)).doesNotThrowAnyException();

        verify(credentialChangePort).changePassword(userId, "old", "Newpass1!");
    }

    @Test
    void changeMyPassword_userNotFound_throwsUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.changeMyPassword(new ChangeMyPasswordCommand(userId, "old", "new")))
            .isInstanceOf(IdentityException.UserNotFound.class);

        verify(credentialChangePort, never()).changePassword(any(), any(), any());
    }

    @Test
    void changeMyPassword_inactiveUser_throwsUserNotActive() {
        User inactive = new User(userId, "u", "u@test.com", Role.USER, false, Instant.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> handler.changeMyPassword(new ChangeMyPasswordCommand(userId, "old", "new")))
            .isInstanceOf(IdentityException.UserNotActive.class);

        verify(credentialChangePort, never()).changePassword(any(), any(), any());
    }
}