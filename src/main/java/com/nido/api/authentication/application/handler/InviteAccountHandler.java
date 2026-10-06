package com.nido.api.authentication.application.handler;

import com.nido.api.authentication.application.dto.InvitationDelivery;
import com.nido.api.authentication.application.port.in.InviteAccountUseCase;
import com.nido.api.authentication.application.service.InvitationIssuer;
import com.nido.api.authentication.domain.model.AccountContact;
import com.nido.api.authentication.domain.model.AuthenticationException;
import com.nido.api.authentication.domain.model.UserProfile;
import com.nido.api.authentication.domain.port.out.AccountInvitationRepository;
import com.nido.api.authentication.domain.port.out.UserCredentialPort;
import com.nido.api.authentication.domain.port.out.UserProfilePort;
import com.nido.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Invites an account identity created, or invites it again. Which accounts may be invited — and by whom — is
 * identity's rule; this only issues the link, in the caller's transaction, and never to an account that already
 * has a password: accepting the link would replace it.
 */
@ApplicationService
public class InviteAccountHandler implements InviteAccountUseCase {

    private static final Logger log = LoggerFactory.getLogger(InviteAccountHandler.class);

    private final UserProfilePort profiles;
    private final AccountInvitationRepository invitations;
    private final UserCredentialPort credentials;
    private final InvitationIssuer issuer;

    public InviteAccountHandler(UserProfilePort profiles, AccountInvitationRepository invitations,
                                UserCredentialPort credentials, InvitationIssuer issuer) {
        this.profiles = profiles;
        this.invitations = invitations;
        this.credentials = credentials;
        this.issuer = issuer;
    }

    @Override
    @Transactional
    public InvitationDelivery invite(UUID userId, String inviterName) {
        UserProfile account = profiles.findById(userId).orElseThrow(AuthenticationException.UserNotFound::new);
        // An invitation sets a password: given to an account that has one, its link would replace it.
        if (credentials.hasCredential(userId)) {
            throw new AuthenticationException.AccountAlreadyJoined();
        }
        InvitationDelivery delivery = issuer.invite(AccountContact.of(account), inviterName);
        log.info("Invitation issued for user {} ({})", userId, how(delivery));
        return delivery;
    }

    @Override
    @Transactional
    public Optional<InvitationDelivery> inviteAgain(UUID userId, String inviterName) {
        // Locked first: an acceptance committing meanwhile is then seen, instead of a replacement that finds no row.
        if (invitations.lockForUser(userId).isEmpty() || credentials.hasCredential(userId)) {
            log.info("Invitation of user {} not issued again: the account has chosen its password", userId);
            return Optional.empty();
        }
        UserProfile account = profiles.findById(userId).orElseThrow(AuthenticationException.UserNotFound::new);
        InvitationDelivery delivery = issuer.invite(AccountContact.of(account), inviterName);
        log.info("Invitation issued again for user {} ({})", userId, how(delivery));
        return Optional.of(delivery);
    }

    private static String how(InvitationDelivery delivery) {
        return delivery instanceof InvitationDelivery.Mailed ? "mailed" : "link handed back";
    }
}
