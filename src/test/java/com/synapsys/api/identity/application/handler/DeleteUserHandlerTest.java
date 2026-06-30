package com.synapsys.api.identity.application.handler;

import com.synapsys.api.identity.domain.model.DeleteUserCommand;
import com.synapsys.api.identity.domain.model.IdentityException;
import com.synapsys.api.identity.domain.model.User;
import com.synapsys.api.identity.domain.port.out.CredentialDeletionPort;
import com.synapsys.api.identity.domain.port.out.TotpDeletionPort;
import com.synapsys.api.identity.domain.port.out.UserCommandPort;
import com.synapsys.api.identity.domain.port.out.UserRepository;
import com.synapsys.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteUserHandlerTest {

    @Mock UserRepository userRepository;
    @Mock UserCommandPort userCommandPort;
    @Mock CredentialDeletionPort credentialDeletionPort;
    @Mock TotpDeletionPort totpDeletionPort;

    private DeleteUserHandler handler;

    private final UUID callerId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new DeleteUserHandler(userRepository, userCommandPort, credentialDeletionPort, totpDeletionPort);
    }

    @Test
    void delete_superAdminDeletesUser_anonymizesAndDeletesRelatedData() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(user(targetId, Role.USER)));

        assertThatCode(() -> handler.delete(new DeleteUserCommand(targetId, callerId, Role.SUPER_ADMIN)))
            .doesNotThrowAnyException();

        InOrder order = inOrder(userCommandPort, credentialDeletionPort, totpDeletionPort);
        order.verify(userCommandPort).deleteGdpr(targetId);
        order.verify(credentialDeletionPort).deleteCredentials(targetId);
        order.verify(totpDeletionPort).deleteTotpData(targetId);
    }

    @Test
    void delete_adminDeletesUser_succeeds() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(user(targetId, Role.USER)));

        assertThatCode(() -> handler.delete(new DeleteUserCommand(targetId, callerId, Role.ADMIN)))
            .doesNotThrowAnyException();

        verify(userCommandPort).deleteGdpr(targetId);
    }

    @Test
    void delete_selfDelete_throwsInsufficientPermissions() {
        assertThatThrownBy(() -> handler.delete(new DeleteUserCommand(callerId, callerId, Role.SUPER_ADMIN)))
            .isInstanceOf(IdentityException.InsufficientPermissions.class);

        verifyNoInteractions(userRepository);
    }

    @Test
    void delete_userNotFound_throwsUserNotFound() {
        when(userRepository.findById(targetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.delete(new DeleteUserCommand(targetId, callerId, Role.SUPER_ADMIN)))
            .isInstanceOf(IdentityException.UserNotFound.class);

        verify(userCommandPort, never()).deleteGdpr(any());
    }

    @Test
    void delete_adminCannotDeleteAdmin_throwsInsufficientPermissions() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(user(targetId, Role.ADMIN)));

        assertThatThrownBy(() -> handler.delete(new DeleteUserCommand(targetId, callerId, Role.ADMIN)))
            .isInstanceOf(IdentityException.InsufficientPermissions.class);

        verify(userCommandPort, never()).deleteGdpr(any());
    }

    private User user(UUID id, Role role) {
        return new User(id, "u-" + id, id + "@test.com", role, true, Instant.now());
    }
}