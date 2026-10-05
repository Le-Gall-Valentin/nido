package com.nido.api.identity.application.handler;

import com.nido.api.identity.application.port.in.RegisterUseCase;
import com.nido.api.identity.domain.model.CreateUserProfileCommand;
import com.nido.api.identity.domain.model.IdentityException;
import com.nido.api.identity.domain.model.InvitationDelivery;
import com.nido.api.identity.domain.model.RegisterCommand;
import com.nido.api.identity.domain.model.RegisteredAccount;
import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.domain.port.out.AccountInvitationPort;
import com.nido.api.identity.domain.port.out.PersonalSpaceInitPort;
import com.nido.api.identity.domain.port.out.TotpRecordInitPort;
import com.nido.api.identity.domain.port.out.UserCommandPort;
import com.nido.api.identity.domain.port.out.UserRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.shared.model.Role;
import com.nido.api.shared.service.RoleHierarchy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class RegisterHandler implements RegisterUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterHandler.class);

    private final UserCommandPort userCommandPort;
    private final UserRepository userRepository;
    private final AccountInvitationPort invitations;
    private final TotpRecordInitPort totpRecordInitPort;
    private final PersonalSpaceInitPort personalSpaceInitPort;

    public RegisterHandler(UserCommandPort userCommandPort,
                           UserRepository userRepository,
                           AccountInvitationPort invitations,
                           TotpRecordInitPort totpRecordInitPort,
                           PersonalSpaceInitPort personalSpaceInitPort) {
        this.userCommandPort = userCommandPort;
        this.userRepository = userRepository;
        this.invitations = invitations;
        this.totpRecordInitPort = totpRecordInitPort;
        this.personalSpaceInitPort = personalSpaceInitPort;
    }

    /**
     * Creates the account without a password and invites it: the holder chooses one with the link, so the
     * administrator never knows it. All in one transaction — a failed invitation leaves no account behind.
     */
    @Override
    @Transactional
    public RegisteredAccount register(RegisterCommand command, UUID callerId, Role callerRole) {
        if (!RoleHierarchy.canManage(callerRole, command.role())) {
            throw new IdentityException.InsufficientPermissions();
        }
        User user = userCommandPort.createProfile(
            new CreateUserProfileCommand(command.username(), command.email(), command.role()));
        totpRecordInitPort.initForUser(user.id());
        personalSpaceInitPort.initForUser(user.id());
        String inviterName = userRepository.findById(callerId).map(User::username).orElse(null);
        InvitationDelivery invitation = invitations.invite(user.id(), inviterName);
        log.info("User {} registered with role {} and invited", user.id(), command.role());
        return new RegisteredAccount(user, invitation);
    }
}
