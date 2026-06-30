package com.synapsys.api.identity.application.handler;

import com.synapsys.api.identity.application.port.in.RegisterUseCase;
import com.synapsys.api.identity.domain.model.CreateUserProfileCommand;
import com.synapsys.api.identity.domain.model.IdentityException;
import com.synapsys.api.identity.domain.model.RegisterCommand;
import com.synapsys.api.identity.domain.model.User;
import com.synapsys.api.identity.domain.port.out.CredentialSetupPort;
import com.synapsys.api.identity.domain.port.out.TotpRecordInitPort;
import com.synapsys.api.identity.domain.port.out.UserCommandPort;
import com.synapsys.api.shared.annotation.ApplicationService;
import com.synapsys.api.shared.model.Role;
import com.synapsys.api.shared.service.RoleHierarchy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class RegisterHandler implements RegisterUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterHandler.class);

    private final UserCommandPort userCommandPort;
    private final CredentialSetupPort credentialSetupPort;
    private final TotpRecordInitPort totpRecordInitPort;

    public RegisterHandler(UserCommandPort userCommandPort,
                           CredentialSetupPort credentialSetupPort,
                           TotpRecordInitPort totpRecordInitPort) {
        this.userCommandPort = userCommandPort;
        this.credentialSetupPort = credentialSetupPort;
        this.totpRecordInitPort = totpRecordInitPort;
    }

    @Override
    @Transactional
    public User register(RegisterCommand command, Role callerRole) {
        if (!RoleHierarchy.canManage(callerRole, command.role())) {
            throw new IdentityException.InsufficientPermissions();
        }
        User user = userCommandPort.createProfile(
            new CreateUserProfileCommand(command.username(), command.email(), command.role()));
        credentialSetupPort.setup(user.id(), command.rawPassword());
        totpRecordInitPort.initForUser(user.id());
        log.info("User {} registered with role {}", user.id(), command.role());
        return user;
    }
}