package com.boilerplate.api.identity.application.handler;

import com.boilerplate.api.identity.application.port.in.UpdateUserUseCase;
import com.boilerplate.api.identity.domain.model.IdentityException;
import com.boilerplate.api.identity.domain.model.UpdateUserCommand;
import com.boilerplate.api.identity.domain.model.User;
import com.boilerplate.api.identity.domain.port.out.UserCommandPort;
import com.boilerplate.api.identity.domain.port.out.UserRepository;
import com.boilerplate.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class UpdateUserHandler implements UpdateUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateUserHandler.class);

    private final UserRepository userRepository;
    private final UserCommandPort userCommandPort;

    public UpdateUserHandler(UserRepository userRepository, UserCommandPort userCommandPort) {
        this.userRepository = userRepository;
        this.userCommandPort = userCommandPort;
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
        log.info("User {} role changed from {} to {} by caller {} with role {}",
            command.targetUserId(), target.role(), command.newRole(), command.callerId(), command.callerRole());
    }
}