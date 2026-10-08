package com.nido.api.identity.application.handler;

import com.nido.api.identity.domain.port.out.AccountInvitationPort;
import com.nido.api.identity.application.service.AdminGestureNotifier;
import com.nido.api.identity.domain.model.DeleteUserCommand;
import com.nido.api.identity.domain.model.IdentityException;
import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.domain.port.out.CredentialDeletionPort;
import com.nido.api.identity.domain.port.out.NotificationDataDeletionPort;
import com.nido.api.identity.domain.port.out.PendingMailCancellationPort;
import com.nido.api.identity.domain.port.out.SpaceDataDeletionPort;
import com.nido.api.identity.domain.port.out.TwoFactorMethodsPort;
import com.nido.api.identity.domain.port.out.TokenInvalidationPort;
import com.nido.api.identity.domain.port.out.UserCommandPort;
import com.nido.api.identity.domain.port.out.UserRepository;
import com.nido.api.shared.model.Role;
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
    @Mock TokenInvalidationPort tokenInvalidationPort;
    @Mock CredentialDeletionPort credentialDeletionPort;
    @Mock TwoFactorMethodsPort twoFactorMethods;
    @Mock SpaceDataDeletionPort spaceDataDeletionPort;
    @Mock NotificationDataDeletionPort notificationDataDeletionPort;
    @Mock PendingMailCancellationPort pendingMailCancellationPort;
    @Mock AccountInvitationPort invitations;
    @Mock AdminGestureNotifier notifier;

    private DeleteUserHandler handler;

    private final UUID callerId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new DeleteUserHandler(userRepository, userCommandPort, credentialDeletionPort, twoFactorMethods,
            spaceDataDeletionPort, notificationDataDeletionPort, pendingMailCancellationPort, tokenInvalidationPort,
            invitations, notifier);
    }

    @Test
    void delete_superAdminDeletesUser_anonymizesAndDeletesRelatedData() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(user(targetId, Role.USER)));

        assertThatCode(() -> handler.delete(new DeleteUserCommand(targetId, callerId, Role.SUPER_ADMIN)))
            .doesNotThrowAnyException();

        InOrder order = inOrder(userCommandPort, credentialDeletionPort, twoFactorMethods);
        order.verify(userCommandPort).deleteGdpr(targetId);
        order.verify(credentialDeletionPort).deleteCredentials(targetId);
        order.verify(twoFactorMethods).deleteAll(targetId);
        verify(spaceDataDeletionPort).deleteSpaceData(targetId, "u-" + targetId);
        verify(notificationDataDeletionPort).deleteNotificationData(targetId);
        verify(pendingMailCancellationPort).cancelPendingMailsTo(targetId + "@test.com");
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
        verifyNoInteractions(notificationDataDeletionPort);
        verifyNoInteractions(pendingMailCancellationPort);
    }

    @Test
    void delete_adminCannotDeleteAdmin_throwsInsufficientPermissions() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(user(targetId, Role.ADMIN)));

        assertThatThrownBy(() -> handler.delete(new DeleteUserCommand(targetId, callerId, Role.ADMIN)))
            .isInstanceOf(IdentityException.InsufficientPermissions.class);

        verify(userCommandPort, never()).deleteGdpr(any());
    }

    private User user(UUID id, Role role) {
        return new User(id, "u-" + id, id + "@test.com", role, true, Instant.now(), null);
    }

    @Test
    void the_goodbye_is_queued_after_the_pending_mails_are_withdrawn_and_knows_whether_the_account_was_only_invited() {
        User target = user(targetId, Role.USER);
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(invitations.isInvited(targetId)).thenReturn(true);

        handler.delete(new DeleteUserCommand(targetId, callerId, Role.ADMIN));

        // Read before the credentials go: deleting them takes the invitation with them.
        InOrder order = inOrder(invitations, credentialDeletionPort, pendingMailCancellationPort, notifier);
        order.verify(invitations).isInvited(targetId);
        order.verify(credentialDeletionPort).deleteCredentials(targetId);
        order.verify(pendingMailCancellationPort).cancelPendingMailsTo(targetId + "@test.com");
        order.verify(notifier).deleted(target, true, callerId, Role.ADMIN);
    }
}
