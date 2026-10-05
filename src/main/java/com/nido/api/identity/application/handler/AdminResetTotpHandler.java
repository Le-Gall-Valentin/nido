package com.nido.api.identity.application.handler;

import com.nido.api.identity.application.service.AdminGestureNotifier;
import com.nido.api.identity.application.port.in.AdminResetTotpUseCase;
import com.nido.api.identity.domain.model.AdminResetTotpCommand;
import com.nido.api.identity.domain.model.IdentityException;
import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.domain.port.out.MfaAdminResetTotpPort;
import com.nido.api.identity.domain.port.out.UserRepository;
import com.nido.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class AdminResetTotpHandler implements AdminResetTotpUseCase {

    private static final Logger log = LoggerFactory.getLogger(AdminResetTotpHandler.class);

    private final UserRepository userRepository;
    private final MfaAdminResetTotpPort mfaResetTotp;
    private final AdminGestureNotifier notifier;

    public AdminResetTotpHandler(UserRepository userRepository, MfaAdminResetTotpPort mfaResetTotp,
                                 AdminGestureNotifier notifier) {
        this.userRepository = userRepository;
        this.mfaResetTotp = mfaResetTotp;
        this.notifier = notifier;
    }

    @Override
    @Transactional
    public void reset(AdminResetTotpCommand command) {
        if (command.callerId().equals(command.targetUserId())) {
            throw new IdentityException.InsufficientPermissions();
        }
        User target = userRepository.findById(command.targetUserId())
            .orElseThrow(IdentityException.UserNotFound::new);
        target.ensureTotpCanBeResetBy(command.callerRole());
        if (mfaResetTotp.disableTotpIfEnabled(command.targetUserId())) {
            notifier.totpReset(target, command.callerId(), command.callerRole());
        }
        log.info("TOTP reset for user {} by caller {}", command.targetUserId(), command.callerId());
    }
}