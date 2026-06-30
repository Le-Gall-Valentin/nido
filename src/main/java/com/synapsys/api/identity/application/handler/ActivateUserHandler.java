package com.synapsys.api.identity.application.handler;

import com.synapsys.api.identity.application.port.in.ActivateUserUseCase;
import com.synapsys.api.identity.domain.model.ActivateUserCommand;
import com.synapsys.api.identity.domain.model.IdentityException;
import com.synapsys.api.identity.domain.model.User;
import com.synapsys.api.identity.domain.port.out.UserCommandPort;
import com.synapsys.api.identity.domain.port.out.UserRepository;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class ActivateUserHandler implements ActivateUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(ActivateUserHandler.class);

    private final UserRepository userRepository;
    private final UserCommandPort userCommandPort;

    public ActivateUserHandler(UserRepository userRepository, UserCommandPort userCommandPort) {
        this.userRepository = userRepository;
        this.userCommandPort = userCommandPort;
    }

    @Override
    @Transactional
    public void activate(ActivateUserCommand command) {
        if (command.targetUserId().equals(command.callerId())) {
            throw new IdentityException.InsufficientPermissions();
        }
        User target = userRepository.findById(command.targetUserId())
            .orElseThrow(IdentityException.UserNotFound::new);
        target.ensureCanBeActivatedBy(command.callerRole());
        target.ensureInactive();
        userCommandPort.activate(command.targetUserId());
        log.info("User {} activated by caller {} with role {}",
            command.targetUserId(), command.callerId(), command.callerRole());
    }
}