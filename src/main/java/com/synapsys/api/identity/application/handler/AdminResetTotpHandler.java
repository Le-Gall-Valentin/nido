package com.synapsys.api.identity.application.handler;

import com.synapsys.api.identity.application.port.in.AdminResetTotpUseCase;
import com.synapsys.api.identity.domain.model.AdminResetTotpCommand;
import com.synapsys.api.identity.domain.model.IdentityException;
import com.synapsys.api.identity.domain.model.User;
import com.synapsys.api.identity.domain.port.out.MfaAdminResetTotpPort;
import com.synapsys.api.identity.domain.port.out.UserRepository;
import com.synapsys.api.shared.annotation.ApplicationService;
import com.synapsys.api.shared.service.RoleHierarchy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class AdminResetTotpHandler implements AdminResetTotpUseCase {

    private static final Logger log = LoggerFactory.getLogger(AdminResetTotpHandler.class);

    private final UserRepository userRepository;
    private final MfaAdminResetTotpPort mfaResetTotp;

    public AdminResetTotpHandler(UserRepository userRepository, MfaAdminResetTotpPort mfaResetTotp) {
        this.userRepository = userRepository;
        this.mfaResetTotp = mfaResetTotp;
    }

    @Override
    @Transactional
    public void reset(AdminResetTotpCommand command) {
        if (command.callerId().equals(command.targetUserId())) {
            throw new IdentityException.InsufficientPermissions();
        }
        User target = userRepository.findById(command.targetUserId())
            .orElseThrow(IdentityException.UserNotFound::new);
        if (!RoleHierarchy.canManage(command.callerRole(), target.role())) {
            throw new IdentityException.InsufficientPermissions();
        }
        mfaResetTotp.disableTotpIfEnabled(command.targetUserId());
        log.info("TOTP reset for user {} by caller {}", command.targetUserId(), command.callerId());
    }
}