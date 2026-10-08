package com.nido.api.identity.application.handler;

import com.nido.api.identity.application.port.in.AdminResetTwoFactorUseCase;
import com.nido.api.identity.application.service.AdminGestureNotifier;
import com.nido.api.identity.domain.model.AdminResetTwoFactorCommand;
import com.nido.api.identity.domain.model.IdentityException;
import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.domain.port.out.TwoFactorMethodsPort;
import com.nido.api.identity.domain.port.out.UserRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.shared.model.TwoFactorMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/** The way out for someone who lost a phone or a mailbox: the administrator removes the method they ticked. */
@ApplicationService
public class AdminResetTwoFactorHandler implements AdminResetTwoFactorUseCase {

    private static final Logger log = LoggerFactory.getLogger(AdminResetTwoFactorHandler.class);

    private final UserRepository userRepository;
    private final TwoFactorMethodsPort twoFactorMethods;
    private final AdminGestureNotifier notifier;

    public AdminResetTwoFactorHandler(UserRepository userRepository, TwoFactorMethodsPort twoFactorMethods,
                                      AdminGestureNotifier notifier) {
        this.userRepository = userRepository;
        this.twoFactorMethods = twoFactorMethods;
        this.notifier = notifier;
    }

    @Override
    @Transactional
    public void reset(AdminResetTwoFactorCommand command) {
        if (command.callerId().equals(command.targetUserId())) {
            throw new IdentityException.InsufficientPermissions();
        }
        User target = userRepository.findById(command.targetUserId())
            .orElseThrow(IdentityException.UserNotFound::new);
        target.ensureTwoFactorCanBeResetBy(command.callerRole());
        Set<TwoFactorMethod> removed = twoFactorMethods.removeByAdmin(target.id(), command.methods());
        if (!removed.isEmpty()) {
            notifier.twoFactorReset(target, removed, twoFactorMethods.activeMethods(target.id()),
                command.callerId(), command.callerRole());
        }
        log.info("Two-factor methods {} reset for user {} by caller {}", removed, command.targetUserId(), command.callerId());
    }
}
