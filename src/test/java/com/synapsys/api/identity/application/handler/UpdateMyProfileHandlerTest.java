package com.synapsys.api.identity.application.handler;

import com.synapsys.api.identity.domain.model.IdentityException;
import com.synapsys.api.identity.domain.model.UpdateProfileCommand;
import com.synapsys.api.identity.domain.model.User;
import com.synapsys.api.identity.domain.port.out.UserCommandPort;
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
class UpdateMyProfileHandlerTest {

    @Mock UserRepository userRepository;
    @Mock UserCommandPort userCommandPort;

    private UpdateMyProfileHandler handler;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new UpdateMyProfileHandler(userRepository, userCommandPort);
    }

    @Test
    void updateProfile_activeUser_delegatesToCommandPort() {
        User user = new User(userId, "old", "old@test.com", Role.USER, true, Instant.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        UpdateProfileCommand cmd = new UpdateProfileCommand(userId, "new", "new@test.com");

        assertThatCode(() -> handler.updateProfile(cmd)).doesNotThrowAnyException();

        verify(userCommandPort).updateProfile(cmd);
    }

    @Test
    void updateProfile_userNotFound_throwsUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.updateProfile(new UpdateProfileCommand(userId, "x", "x@test.com")))
            .isInstanceOf(IdentityException.UserNotFound.class);

        verify(userCommandPort, never()).updateProfile(any(UpdateProfileCommand.class));
    }

    @Test
    void updateProfile_inactiveUser_throwsUserNotActive() {
        User inactive = new User(userId, "old", "old@test.com", Role.USER, false, Instant.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> handler.updateProfile(new UpdateProfileCommand(userId, "x", "x@test.com")))
            .isInstanceOf(IdentityException.UserNotActive.class);

        verify(userCommandPort, never()).updateProfile(any(UpdateProfileCommand.class));
    }
}