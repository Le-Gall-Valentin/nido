package com.nido.api.identity.application.handler;

import com.nido.api.identity.application.service.AdminGestureNotifier;
import com.nido.api.identity.application.port.in.ActivateUserUseCase;
import com.nido.api.identity.domain.model.ActivateUserCommand;
import com.nido.api.identity.domain.model.IdentityException;
import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.domain.port.out.UserCommandPort;
import com.nido.api.identity.domain.port.out.UserRepository;
import com.nido.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class ActivateUserHandler implements ActivateUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(ActivateUserHandler.class);

    private final UserRepository userRepository;
    private final UserCommandPort userCommandPort;
    private final AdminGestureNotifier notifier;

    public ActivateUserHandler(UserRepository userRepository, UserCommandPort userCommandPort,
                               AdminGestureNotifier notifier) {
        this.userRepository = userRepository;
        this.userCommandPort = userCommandPort;
        this.notifier = notifier;
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
        // As for a deactivation: another administrator may have reactivated it since it was loaded.
        if (!userCommandPort.activate(command.targetUserId())) {
            throw new IdentityException.UserAlreadyActive();
        }
        notifier.reactivated(target, command.callerId(), command.callerRole());
        log.info("User {} activated by caller {} with role {}",
            command.targetUserId(), command.callerId(), command.callerRole());
    }
}