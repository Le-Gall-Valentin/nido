package com.nido.api.identity.application.handler;

import com.nido.api.identity.application.port.in.DeleteUserUseCase;
import com.nido.api.identity.application.service.AdminGestureNotifier;
import com.nido.api.identity.domain.model.DeleteUserCommand;
import com.nido.api.identity.domain.model.IdentityException;
import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.domain.port.out.AccountInvitationPort;
import com.nido.api.identity.domain.port.out.CredentialDeletionPort;
import com.nido.api.identity.domain.port.out.NotificationDataDeletionPort;
import com.nido.api.identity.domain.port.out.PendingMailCancellationPort;
import com.nido.api.identity.domain.port.out.SpaceDataDeletionPort;
import com.nido.api.identity.domain.port.out.TotpDeletionPort;
import com.nido.api.identity.domain.port.out.TokenInvalidationPort;
import com.nido.api.identity.domain.port.out.UserCommandPort;
import com.nido.api.identity.domain.port.out.UserRepository;
import com.nido.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class DeleteUserHandler implements DeleteUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteUserHandler.class);

    private final UserRepository userRepository;
    private final UserCommandPort userCommandPort;
    private final CredentialDeletionPort credentialDeletionPort;
    private final TotpDeletionPort totpDeletionPort;
    private final SpaceDataDeletionPort spaceDataDeletionPort;
    private final NotificationDataDeletionPort notificationDataDeletionPort;
    private final PendingMailCancellationPort pendingMailCancellationPort;
    private final TokenInvalidationPort tokenInvalidationPort;
    private final AccountInvitationPort invitations;
    private final AdminGestureNotifier notifier;

    public DeleteUserHandler(UserRepository userRepository,
                             UserCommandPort userCommandPort,
                             CredentialDeletionPort credentialDeletionPort,
                             TotpDeletionPort totpDeletionPort,
                             SpaceDataDeletionPort spaceDataDeletionPort,
                             NotificationDataDeletionPort notificationDataDeletionPort,
                             PendingMailCancellationPort pendingMailCancellationPort,
                             TokenInvalidationPort tokenInvalidationPort,
                             AccountInvitationPort invitations,
                             AdminGestureNotifier notifier) {
        this.userRepository = userRepository;
        this.userCommandPort = userCommandPort;
        this.credentialDeletionPort = credentialDeletionPort;
        this.totpDeletionPort = totpDeletionPort;
        this.spaceDataDeletionPort = spaceDataDeletionPort;
        this.notificationDataDeletionPort = notificationDataDeletionPort;
        this.pendingMailCancellationPort = pendingMailCancellationPort;
        this.tokenInvalidationPort = tokenInvalidationPort;
        this.invitations = invitations;
        this.notifier = notifier;
    }

    @Override
    @Transactional
    public void delete(DeleteUserCommand command) {
        if (command.targetUserId().equals(command.callerId())) {
            throw new IdentityException.InsufficientPermissions();
        }
        User target = userRepository.findById(command.targetUserId())
            .orElseThrow(IdentityException.UserNotFound::new);
        target.ensureCanBeDeletedBy(command.callerRole());
        // Read before the credentials go: deleting them takes the invitation with them.
        boolean wasInvited = invitations.isInvited(target.id());
        userCommandPort.deleteGdpr(command.targetUserId());
        credentialDeletionPort.deleteCredentials(command.targetUserId());
        totpDeletionPort.deleteTotpData(command.targetUserId());
        spaceDataDeletionPort.deleteSpaceData(command.targetUserId(), target.username());
        notificationDataDeletionPort.deleteNotificationData(command.targetUserId());
        // Read before the anonymisation above wiped it from the row; still in hand in the loaded account.
        if (target.email() != null) {
            pendingMailCancellationPort.cancelPendingMailsTo(target.email());
        }
        // Everything about the user is gone, except the access token in their browser — nothing in
        // it consults the database, so it would keep authenticating a user who no longer exists.
        tokenInvalidationPort.invalidateIssuedTokens(command.targetUserId());
        // Queued after the withdrawal of the mails still waiting for this address, or it would withdraw itself.
        notifier.deleted(target, wasInvited, command.callerId(), command.callerRole());
        log.info("GDPR delete performed by caller {} with role {}",
            command.callerId(), command.callerRole());
    }
}