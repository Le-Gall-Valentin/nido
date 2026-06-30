package com.synapsys.api.identity.application.handler;

import com.synapsys.api.identity.application.port.in.DeleteUserUseCase;
import com.synapsys.api.identity.domain.model.DeleteUserCommand;
import com.synapsys.api.identity.domain.model.IdentityException;
import com.synapsys.api.identity.domain.model.User;
import com.synapsys.api.identity.domain.port.out.CredentialDeletionPort;
import com.synapsys.api.identity.domain.port.out.TotpDeletionPort;
import com.synapsys.api.identity.domain.port.out.UserCommandPort;
import com.synapsys.api.identity.domain.port.out.UserRepository;
import com.synapsys.api.shared.annotation.ApplicationService;
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

    public DeleteUserHandler(UserRepository userRepository,
                             UserCommandPort userCommandPort,
                             CredentialDeletionPort credentialDeletionPort,
                             TotpDeletionPort totpDeletionPort) {
        this.userRepository = userRepository;
        this.userCommandPort = userCommandPort;
        this.credentialDeletionPort = credentialDeletionPort;
        this.totpDeletionPort = totpDeletionPort;
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
        userCommandPort.deleteGdpr(command.targetUserId());
        credentialDeletionPort.deleteCredentials(command.targetUserId());
        totpDeletionPort.deleteTotpData(command.targetUserId());
        log.info("GDPR delete performed by caller {} with role {}",
            command.callerId(), command.callerRole());
    }
}