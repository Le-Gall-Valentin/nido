package com.nido.api.identity.application.handler;

import com.nido.api.identity.application.port.in.ResendInvitationUseCase;
import com.nido.api.identity.domain.model.IdentityException;
import com.nido.api.identity.domain.model.InvitationDelivery;
import com.nido.api.identity.domain.model.ResendInvitationCommand;
import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.domain.port.out.AccountInvitationPort;
import com.nido.api.identity.domain.port.out.UserRepository;
import com.nido.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * A new invitation link for an account that has not chosen its password yet — an expired link, a lost mail.
 * Under the rules of every other gesture on an account; a deactivated account is refused, since its link
 * would be refused too.
 */
@ApplicationService
public class ResendInvitationHandler implements ResendInvitationUseCase {

    private static final Logger log = LoggerFactory.getLogger(ResendInvitationHandler.class);

    private final UserRepository userRepository;
    private final AccountInvitationPort invitations;

    public ResendInvitationHandler(UserRepository userRepository, AccountInvitationPort invitations) {
        this.userRepository = userRepository;
        this.invitations = invitations;
    }

    @Override
    @Transactional
    public InvitationDelivery resend(ResendInvitationCommand command) {
        if (command.targetUserId().equals(command.callerId())) {
            throw new IdentityException.InsufficientPermissions();
        }
        User target = userRepository.findById(command.targetUserId())
            .orElseThrow(IdentityException.UserNotFound::new);
        target.ensureInvitationCanBeResentBy(command.callerRole());
        if (!target.isActive()) {
            throw new IdentityException.UserNotActive();
        }
        if (!invitations.isInvited(target.id())) {
            throw new IdentityException.AccountAlreadyJoined();
        }
        String inviterName = userRepository.findById(command.callerId()).map(User::username).orElse(null);
        InvitationDelivery delivery = invitations.invite(target.id(), inviterName);
        log.info("Invitation of user {} issued again by caller {}", target.id(), command.callerId());
        return delivery;
    }
}
