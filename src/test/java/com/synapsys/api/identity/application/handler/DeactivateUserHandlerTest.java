package com.synapsys.api.identity.application.handler;

import com.synapsys.api.identity.domain.model.DeactivateUserCommand;
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
class DeactivateUserHandlerTest {

    @Mock UserRepository userRepository;
    @Mock UserCommandPort userCommandPort;

    private DeactivateUserHandler handler;

    private final UUID callerId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new DeactivateUserHandler(userRepository, userCommandPort);
    }

    @Test
    void deactivate_superAdminDeactivatesAdmin_succeeds() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(user(targetId, Role.ADMIN)));

        assertThatCode(() -> handler.deactivate(new DeactivateUserCommand(targetId, callerId, Role.SUPER_ADMIN)))
            .doesNotThrowAnyException();

        verify(userCommandPort).deactivate(targetId);
    }

    @Test
    void deactivate_superAdminDeactivatesUser_succeeds() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(user(targetId, Role.USER)));

        assertThatCode(() -> handler.deactivate(new DeactivateUserCommand(targetId, callerId, Role.SUPER_ADMIN)))
            .doesNotThrowAnyException();

        verify(userCommandPort).deactivate(targetId);
    }

    @Test
    void deactivate_adminDeactivatesUser_succeeds() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(user(targetId, Role.USER)));

        assertThatCode(() -> handler.deactivate(new DeactivateUserCommand(targetId, callerId, Role.ADMIN)))
            .doesNotThrowAnyException();

        verify(userCommandPort).deactivate(targetId);
    }

    @Test
    void deactivate_adminCannotDeactivateAdmin_throwsInsufficientPermissions() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(user(targetId, Role.ADMIN)));

        assertThatThrownBy(() -> handler.deactivate(new DeactivateUserCommand(targetId, callerId, Role.ADMIN)))
            .isInstanceOf(IdentityException.InsufficientPermissions.class);

        verify(userCommandPort, never()).deactivate(any());
    }

    @Test
    void deactivate_adminCannotDeactivateSuperAdmin_throwsInsufficientPermissions() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(user(targetId, Role.SUPER_ADMIN)));

        assertThatThrownBy(() -> handler.deactivate(new DeactivateUserCommand(targetId, callerId, Role.ADMIN)))
            .isInstanceOf(IdentityException.InsufficientPermissions.class);

        verify(userCommandPort, never()).deactivate(any());
    }

    @Test
    void deactivate_superAdminCannotDeactivateAnotherSuperAdmin_throwsInsufficientPermissions() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(user(targetId, Role.SUPER_ADMIN)));

        assertThatThrownBy(() -> handler.deactivate(new DeactivateUserCommand(targetId, callerId, Role.SUPER_ADMIN)))
            .isInstanceOf(IdentityException.InsufficientPermissions.class);

        verify(userCommandPort, never()).deactivate(any());
    }

    @Test
    void deactivate_selfDeactivation_throwsInsufficientPermissions() {
        assertThatThrownBy(() -> handler.deactivate(new DeactivateUserCommand(callerId, callerId, Role.SUPER_ADMIN)))
            .isInstanceOf(IdentityException.InsufficientPermissions.class)
            .hasMessageContaining("Insufficient permissions");

        verifyNoInteractions(userRepository);
    }

    @Test
    void deactivate_targetNotFound_throwsUserNotFound() {
        when(userRepository.findById(targetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.deactivate(new DeactivateUserCommand(targetId, callerId, Role.SUPER_ADMIN)))
            .isInstanceOf(IdentityException.UserNotFound.class);

        verify(userCommandPort, never()).deactivate(any());
    }

    @Test
    void deactivate_alreadyInactiveUser_throwsUserAlreadyInactive() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(inactiveUser(targetId, Role.USER)));

        assertThatThrownBy(() -> handler.deactivate(new DeactivateUserCommand(targetId, callerId, Role.SUPER_ADMIN)))
            .isInstanceOf(IdentityException.UserAlreadyInactive.class);

        verify(userCommandPort, never()).deactivate(any());
    }

    private User user(UUID id, Role role) {
        return new User(id, "user-" + id, id + "@test.com", role, true, Instant.now());
    }

    private User inactiveUser(UUID id, Role role) {
        return new User(id, "user-" + id, id + "@test.com", role, false, Instant.now());
    }
}