package com.synapsys.api.identity.application.handler;

import com.synapsys.api.identity.domain.model.ActivateUserCommand;
import com.synapsys.api.identity.domain.model.IdentityException;
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
class ActivateUserHandlerTest {

    @Mock UserRepository userRepository;
    @Mock UserCommandPort userCommandPort;

    private ActivateUserHandler handler;

    private final UUID callerId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ActivateUserHandler(userRepository, userCommandPort);
    }

    @Test
    void activate_superAdminActivatesInactiveUser_succeeds() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(inactive(targetId, Role.USER)));

        assertThatCode(() -> handler.activate(new ActivateUserCommand(targetId, callerId, Role.SUPER_ADMIN)))
            .doesNotThrowAnyException();

        verify(userCommandPort).activate(targetId);
    }

    @Test
    void activate_adminActivatesInactiveUser_succeeds() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(inactive(targetId, Role.USER)));

        assertThatCode(() -> handler.activate(new ActivateUserCommand(targetId, callerId, Role.ADMIN)))
            .doesNotThrowAnyException();

        verify(userCommandPort).activate(targetId);
    }

    @Test
    void activate_selfActivation_throwsInsufficientPermissions() {
        assertThatThrownBy(() -> handler.activate(new ActivateUserCommand(callerId, callerId, Role.SUPER_ADMIN)))
            .isInstanceOf(IdentityException.InsufficientPermissions.class);

        verifyNoInteractions(userRepository);
    }

    @Test
    void activate_targetNotFound_throwsUserNotFound() {
        when(userRepository.findById(targetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.activate(new ActivateUserCommand(targetId, callerId, Role.SUPER_ADMIN)))
            .isInstanceOf(IdentityException.UserNotFound.class);

        verify(userCommandPort, never()).activate(any());
    }

    @Test
    void activate_alreadyActiveUser_throwsUserAlreadyActive() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(active(targetId, Role.USER)));

        assertThatThrownBy(() -> handler.activate(new ActivateUserCommand(targetId, callerId, Role.SUPER_ADMIN)))
            .isInstanceOf(IdentityException.UserAlreadyActive.class);

        verify(userCommandPort, never()).activate(any());
    }

    @Test
    void activate_adminCannotActivateAdmin_throwsInsufficientPermissions() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(inactive(targetId, Role.ADMIN)));

        assertThatThrownBy(() -> handler.activate(new ActivateUserCommand(targetId, callerId, Role.ADMIN)))
            .isInstanceOf(IdentityException.InsufficientPermissions.class);

        verify(userCommandPort, never()).activate(any());
    }

    @Test
    void activate_adminCannotActivateActiveSuperAdmin_throwsInsufficientPermissions_notUserAlreadyActive() {
        // Role hierarchy check must fire before state check — admin must not learn the target's state
        when(userRepository.findById(targetId)).thenReturn(Optional.of(active(targetId, Role.SUPER_ADMIN)));

        assertThatThrownBy(() -> handler.activate(new ActivateUserCommand(targetId, callerId, Role.ADMIN)))
            .isInstanceOf(IdentityException.InsufficientPermissions.class);

        verify(userCommandPort, never()).activate(any());
    }

    private User active(UUID id, Role role) {
        return new User(id, "u-" + id, id + "@test.com", role, true, Instant.now());
    }

    private User inactive(UUID id, Role role) {
        return new User(id, "u-" + id, id + "@test.com", role, false, Instant.now());
    }
}