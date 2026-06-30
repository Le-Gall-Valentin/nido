package com.synapsys.api.identity.application.handler;

import com.synapsys.api.identity.domain.model.IdentityException;
import com.synapsys.api.identity.domain.model.UpdateUserCommand;
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
class UpdateUserHandlerTest {

    @Mock UserRepository userRepository;
    @Mock UserCommandPort userCommandPort;

    private UpdateUserHandler handler;

    private final UUID callerId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new UpdateUserHandler(userRepository, userCommandPort);
    }

    @Test
    void update_superAdminChangesUserRoleToAdmin_succeeds() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(active(targetId, Role.USER)));

        assertThatCode(() -> handler.update(new UpdateUserCommand(targetId, callerId, Role.SUPER_ADMIN, Role.ADMIN)))
            .doesNotThrowAnyException();

        verify(userCommandPort).updateRole(targetId, Role.USER, Role.ADMIN);
    }

    @Test
    void update_superAdminChangesAdminRoleToUser_succeeds() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(active(targetId, Role.ADMIN)));

        assertThatCode(() -> handler.update(new UpdateUserCommand(targetId, callerId, Role.SUPER_ADMIN, Role.USER)))
            .doesNotThrowAnyException();

        verify(userCommandPort).updateRole(targetId, Role.ADMIN, Role.USER);
    }

    @Test
    void update_selfModification_throwsInsufficientPermissions() {
        assertThatThrownBy(() -> handler.update(new UpdateUserCommand(callerId, callerId, Role.SUPER_ADMIN, Role.USER)))
            .isInstanceOf(IdentityException.InsufficientPermissions.class);

        verifyNoInteractions(userRepository);
        verifyNoInteractions(userCommandPort);
    }

    @Test
    void update_targetNotFound_throwsUserNotFound() {
        when(userRepository.findById(targetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.update(new UpdateUserCommand(targetId, callerId, Role.SUPER_ADMIN, Role.USER)))
            .isInstanceOf(IdentityException.UserNotFound.class);

        verify(userCommandPort, never()).updateRole(any(), any(), any());
    }

    @Test
    void update_inactiveTarget_throwsUserNotActive() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(inactive(targetId, Role.USER)));

        assertThatThrownBy(() -> handler.update(new UpdateUserCommand(targetId, callerId, Role.SUPER_ADMIN, Role.ADMIN)))
            .isInstanceOf(IdentityException.UserNotActive.class);

        verify(userCommandPort, never()).updateRole(any(), any(), any());
    }

    @Test
    void update_adminCannotUpdateAdmin_throwsInsufficientPermissions() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(active(targetId, Role.ADMIN)));

        assertThatThrownBy(() -> handler.update(new UpdateUserCommand(targetId, callerId, Role.ADMIN, Role.USER)))
            .isInstanceOf(IdentityException.InsufficientPermissions.class);

        verify(userCommandPort, never()).updateRole(any(), any(), any());
    }

    @Test
    void update_adminCannotAssignAdminRole_throwsInsufficientPermissions() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(active(targetId, Role.USER)));

        assertThatThrownBy(() -> handler.update(new UpdateUserCommand(targetId, callerId, Role.ADMIN, Role.ADMIN)))
            .isInstanceOf(IdentityException.InsufficientPermissions.class);

        verify(userCommandPort, never()).updateRole(any(), any(), any());
    }

    @Test
    void update_noneCanAssignSuperAdminRole_throwsInsufficientPermissions() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(active(targetId, Role.USER)));

        assertThatThrownBy(() -> handler.update(new UpdateUserCommand(targetId, callerId, Role.SUPER_ADMIN, Role.SUPER_ADMIN)))
            .isInstanceOf(IdentityException.InsufficientPermissions.class);

        verify(userCommandPort, never()).updateRole(any(), any(), any());
    }

    @Test
    void update_userCallerCannotUpdateAnyone_throwsInsufficientPermissions() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(active(targetId, Role.USER)));

        assertThatThrownBy(() -> handler.update(new UpdateUserCommand(targetId, callerId, Role.USER, Role.USER)))
            .isInstanceOf(IdentityException.InsufficientPermissions.class);

        verify(userCommandPort, never()).updateRole(any(), any(), any());
    }

    @Test
    void update_sameRole_throwsRoleAlreadyAssigned() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(active(targetId, Role.USER)));

        assertThatThrownBy(() -> handler.update(new UpdateUserCommand(targetId, callerId, Role.SUPER_ADMIN, Role.USER)))
            .isInstanceOf(IdentityException.RoleAlreadyAssigned.class);

        verify(userCommandPort, never()).updateRole(any(), any(), any());
    }

    @Test
    void update_adminCannotLearnSuperAdminExists_throwsInsufficientPermissionsNotUserNotFound() {
        // isActive check fires before role hierarchy — admin must not learn the target's state
        when(userRepository.findById(targetId)).thenReturn(Optional.of(active(targetId, Role.SUPER_ADMIN)));

        assertThatThrownBy(() -> handler.update(new UpdateUserCommand(targetId, callerId, Role.ADMIN, Role.USER)))
            .isInstanceOf(IdentityException.InsufficientPermissions.class);

        verify(userCommandPort, never()).updateRole(any(), any(), any());
    }

    private User active(UUID id, Role role) {
        return new User(id, "u-" + id, id + "@test.com", role, true, Instant.now());
    }

    private User inactive(UUID id, Role role) {
        return new User(id, "u-" + id, id + "@test.com", role, false, Instant.now());
    }
}