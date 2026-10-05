package com.nido.api.authentication.application.handler;

import com.nido.api.authentication.application.dto.InvitationDelivery;
import com.nido.api.authentication.application.port.in.InviteAccountUseCase;
import com.nido.api.authentication.application.service.InvitationIssuer;
import com.nido.api.authentication.domain.model.AccountContact;
import com.nido.api.authentication.domain.model.AuthenticationException;
import com.nido.api.authentication.domain.model.UserProfile;
import com.nido.api.authentication.domain.port.out.UserProfilePort;
import com.nido.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Invites an account identity created, or invites it again. Which accounts may be invited — and by whom — is
 * identity's rule; this only issues the link, in the caller's transaction.
 */
@ApplicationService
public class InviteAccountHandler implements InviteAccountUseCase {

    private static final Logger log = LoggerFactory.getLogger(InviteAccountHandler.class);

    private final UserProfilePort profiles;
    private final InvitationIssuer issuer;

    public InviteAccountHandler(UserProfilePort profiles, InvitationIssuer issuer) {
        this.profiles = profiles;
        this.issuer = issuer;
    }

    @Override
    @Transactional
    public InvitationDelivery invite(UUID userId, String inviterName) {
        UserProfile account = profiles.findById(userId).orElseThrow(AuthenticationException.UserNotFound::new);
        InvitationDelivery delivery = issuer.issue(AccountContact.of(account), inviterName);
        log.info("Invitation issued for user {} ({})", userId,
            delivery instanceof InvitationDelivery.Mailed ? "mailed" : "link handed back");
        return delivery;
    }
}
