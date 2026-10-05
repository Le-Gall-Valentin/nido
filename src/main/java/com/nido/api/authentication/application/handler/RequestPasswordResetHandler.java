package com.nido.api.authentication.application.handler;

import com.nido.api.authentication.application.service.InvitationIssuer;
import com.nido.api.authentication.domain.model.AccountInvitation;
import com.nido.api.authentication.domain.port.out.AccountInvitationRepository;
import com.nido.api.authentication.application.port.in.RequestPasswordResetUseCase;
import com.nido.api.authentication.domain.model.AccountContact;
import com.nido.api.authentication.domain.model.PasswordResetRules;
import com.nido.api.authentication.domain.model.UserProfile;
import com.nido.api.authentication.domain.port.out.AccountLockPort;
import com.nido.api.authentication.domain.port.out.AccountMailPort;
import com.nido.api.authentication.domain.port.out.PasswordResetTokenRepository;
import com.nido.api.authentication.domain.port.out.ResetTokenGeneratorPort;
import com.nido.api.authentication.domain.port.out.TokenHashPort;
import com.nido.api.authentication.domain.port.out.UserProfilePort;
import com.nido.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * "Forgot password": finds the account behind a username or an address and mails it a link.
 *
 * <p>Whatever happens, the caller learns nothing — the route answers the same way for an account, a
 * stranger, a deactivated account and a cooldown — so nothing here throws for those cases, and the
 * logs never repeat what was typed.
 *
 * <p>Which account an identifier names — a username, or an address whatever its letter case — is
 * identity's rule ({@code FindUserUseCase#findByIdentifier}); an address belongs to one account at most.
 *
 * <p>An invited account — no password yet — gets a new invitation instead of a reset link: the same answer
 * for the caller, the same pace.
 */
@ApplicationService
public class RequestPasswordResetHandler implements RequestPasswordResetUseCase {

    private static final Logger log = LoggerFactory.getLogger(RequestPasswordResetHandler.class);

    private final UserProfilePort profiles;
    private final PasswordResetTokenRepository tokens;
    private final AccountInvitationRepository invitations;
    private final InvitationIssuer issuer;
    private final ResetTokenGeneratorPort generator;
    private final TokenHashPort hasher;
    private final AccountMailPort mail;
    private final AccountLockPort accountLock;
    private final Clock clock;

    public RequestPasswordResetHandler(UserProfilePort profiles, PasswordResetTokenRepository tokens,
                                       AccountInvitationRepository invitations, InvitationIssuer issuer,
                                       ResetTokenGeneratorPort generator, TokenHashPort hasher,
                                       AccountMailPort mail, AccountLockPort accountLock, Clock clock) {
        this.profiles = profiles;
        this.tokens = tokens;
        this.invitations = invitations;
        this.issuer = issuer;
        this.generator = generator;
        this.hasher = hasher;
        this.mail = mail;
        this.accountLock = accountLock;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void request(String identifier) {
        String typed = identifier == null ? "" : identifier.strip();
        if (typed.isEmpty()) {
            return;
        }
        Optional<UserProfile> found = profiles.findByIdentifier(typed);
        if (found.isEmpty() || !found.get().isActive()) {
            log.info("Password reset asked for no active account");
            return;
        }
        UserProfile account = found.get();
        // Two requests at the same moment would both find no recent link and both send one: the second
        // waits here until the first has committed its link, then sees it and holds back.
        accountLock.lockFor(account.id());
        Instant now = clock.instant();
        Optional<AccountInvitation> invitation = invitations.findByUserId(account.id());
        if (invitation.isPresent()) {
            renewInvitation(account, invitation.get(), now);
            return;
        }
        if (tokens.latestIssuedAt(account.id()).filter(last -> PasswordResetRules.inCooldown(last, now)).isPresent()) {
            log.info("Password reset for user {} held back: a link went out less than {} ago",
                account.id(), PasswordResetRules.COOLDOWN);
            return;
        }
        tokens.deleteAllForUser(account.id());
        String token = generator.newToken();
        Instant expiresAt = now.plus(PasswordResetRules.VALIDITY);
        tokens.save(account.id(), hasher.hash(token), now, expiresAt);
        mail.passwordResetRequested(AccountContact.of(account), token, expiresAt, PasswordResetRules.VALIDITY);
        log.info("Password reset link issued for user {}", account.id());
    }

    /**
     * An invited account has no password to reset: it gets a new invitation instead, at the pace of reset
     * links. Mail is on — the reset routes do not exist otherwise — so the link is mailed, never handed back.
     */
    private void renewInvitation(UserProfile account, AccountInvitation invitation, Instant now) {
        if (PasswordResetRules.inCooldown(invitation.createdAt(), now)) {
            log.info("Invitation of user {} held back: one went out less than {} ago", account.id(),
                PasswordResetRules.COOLDOWN);
            return;
        }
        issuer.issue(AccountContact.of(account), null);
        log.info("Invitation of user {} renewed from forgot password", account.id());
    }
}
