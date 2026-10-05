package com.nido.api.authentication.application.service;

import com.nido.api.authentication.application.dto.InvitationDelivery;
import com.nido.api.authentication.domain.model.AccountContact;
import com.nido.api.authentication.domain.model.AccountInvitationRules;
import com.nido.api.authentication.domain.port.out.AccountInvitationRepository;
import com.nido.api.authentication.domain.port.out.AccountMailPort;
import com.nido.api.authentication.domain.port.out.PublicUrlPort;
import com.nido.api.authentication.domain.port.out.ResetTokenGeneratorPort;
import com.nido.api.authentication.domain.port.out.TokenHashPort;
import com.nido.api.shared.annotation.ApplicationService;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Gives an account a new invitation link, replacing any previous one, and gets it to its holder: by mail when
 * mail is on, otherwise handed back for the administrator to pass on. Only the token's hash is stored, so a
 * link handed back once can never be shown again.
 *
 * <p>Runs in the caller's transaction: an account creation that rolls back leaves no invitation behind, and its
 * mail, queued in the same transaction, never leaves.
 */
@ApplicationService
public class InvitationIssuer {

    private final AccountInvitationRepository invitations;
    private final ResetTokenGeneratorPort generator;
    private final TokenHashPort hasher;
    private final AccountMailPort mail;
    private final PublicUrlPort publicUrl;
    private final Clock clock;

    public InvitationIssuer(AccountInvitationRepository invitations, ResetTokenGeneratorPort generator,
                            TokenHashPort hasher, AccountMailPort mail, PublicUrlPort publicUrl, Clock clock) {
        this.invitations = invitations;
        this.generator = generator;
        this.hasher = hasher;
        this.mail = mail;
        this.publicUrl = publicUrl;
        this.clock = clock;
    }

    /**
     * A new link for the account, replacing any previous one: mailed when mail is on, otherwise handed back.
     *
     * @param inviterName the administrator named in the mail; never null
     */
    public InvitationDelivery invite(AccountContact account, String inviterName) {
        Objects.requireNonNull(inviterName, "An invitation names who sends it");
        NewLink link = newLink(account);
        if (mail.canSend()) {
            mail.accountInvitation(account, link.token(), link.expiresAt(), inviterName);
            return new InvitationDelivery.Mailed();
        }
        return new InvitationDelivery.Link(publicUrl.publicUrl().orElse("") + AccountInvitationRules.welcomePath(link.token()));
    }

    /**
     * A new link the account asked for from "forgot password", replacing its previous one. It can only leave by
     * mail: with mail off, nothing changes — a link an administrator handed over keeps working.
     *
     * @return whether a new link went out
     */
    public boolean renew(AccountContact account) {
        if (!mail.canSend()) {
            return false;
        }
        NewLink link = newLink(account);
        mail.invitationRenewed(account, link.token(), link.expiresAt());
        return true;
    }

    /** Only the token's hash is stored: the raw token lives as long as this call. */
    private NewLink newLink(AccountContact account) {
        String token = generator.newToken();
        Instant now = clock.instant();
        Instant expiresAt = now.plus(AccountInvitationRules.VALIDITY);
        invitations.save(account.userId(), hasher.hash(token), now, expiresAt);
        return new NewLink(token, expiresAt);
    }

    private record NewLink(String token, Instant expiresAt) {
        @Override
        public String toString() {
            return "NewLink[expiresAt=" + expiresAt + "]";
        }
    }
}
