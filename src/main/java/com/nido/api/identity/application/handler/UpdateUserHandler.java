package com.nido.api.identity.application.handler;

import com.nido.api.identity.application.port.in.UpdateUserUseCase;
import com.nido.api.identity.domain.model.IdentityException;
import com.nido.api.identity.domain.model.UpdateUserCommand;
import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.domain.port.out.TokenInvalidationPort;
import com.nido.api.identity.domain.port.out.UserCommandPort;
import com.nido.api.identity.domain.port.out.UserRepository;
import com.nido.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class UpdateUserHandler implements UpdateUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateUserHandler.class);

    private final UserRepository userRepository;
    private final UserCommandPort userCommandPort;
    private final TokenInvalidationPort tokenInvalidationPort;

    public UpdateUserHandler(UserRepository userRepository, UserCommandPort userCommandPort,
                             TokenInvalidationPort tokenInvalidationPort) {
        this.userRepository = userRepository;
        this.userCommandPort = userCommandPort;
        this.tokenInvalidationPort = tokenInvalidationPort;
    }

    @Override
    @Transactional
    public void update(UpdateUserCommand command) {
        if (command.targetUserId().equals(command.callerId())) {
            throw new IdentityException.InsufficientPermissions();
        }
        User target = userRepository.findById(command.targetUserId())
            .orElseThrow(IdentityException.UserNotFound::new);
        if (!target.isActive()) {
            throw new IdentityException.UserNotActive();
        }
        target.ensureCanBeUpdatedBy(command.callerRole());
        target.ensureRoleCanBeAssignedBy(command.callerRole(), command.newRole());
        target.ensureRoleNotAlreadyAssigned(command.newRole());
        userCommandPort.updateRole(command.targetUserId(), target.role(), command.newRole());
        // The new role is in the database, but the demoted user is still carrying a token that
        // says the old one. Without this, it keeps saying it until that token expires.
        tokenInvalidationPort.invalidateIssuedTokens(command.targetUserId());
        log.info("User {} role changed from {} to {} by caller {} with role {}",
            command.targetUserId(), target.role(), command.newRole(), command.callerId(), command.callerRole());
    }
}