package com.nido.api.authentication.infrastructure.mail;

import com.nido.api.authentication.domain.model.AccountContact;
import com.nido.api.authentication.domain.model.AccountInvitationRules;
import com.nido.api.authentication.domain.port.out.AccountMailPort;
import com.nido.api.infrastructure.web.MailLanguage;
import com.nido.api.mail.application.port.in.MailAvailabilityQuery;
import com.nido.api.mail.application.port.in.SendMailUseCase;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.mail.domain.model.MailRequest;
import com.nido.api.mail.domain.model.Recipient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Authentication's account mails, sent through the mail context. The reset link carries its token
 * after the '#': that part never reaches a server, so the token appears in no access log and no
 * Referer header.
 */
@Component
public class AccountMailAdapter implements AccountMailPort {

    private static final Logger log = LoggerFactory.getLogger(AccountMailAdapter.class);
    /** An alert that outlives its moment is worse than none: a queue kept while mail was off must not deliver it weeks late. */
    private static final Duration ALERT_VALIDITY = Duration.ofHours(24);
    private static final AppPath LOGIN = new AppPath("/login");
    private static final long VALIDITY_DAYS = AccountInvitationRules.VALIDITY.toDays();

    private final SendMailUseCase sendMail;
    private final MailAvailabilityQuery availability;
    private final Clock clock;

    public AccountMailAdapter(SendMailUseCase sendMail, MailAvailabilityQuery availability, Clock clock) {
        this.sendMail = sendMail;
        this.availability = availability;
        this.clock = clock;
    }

    @Override
    public boolean canSend() {
        return availability.isAvailable();
    }

    @Override
    public void passwordResetRequested(AccountContact account, String rawToken, Instant expiresAt, Duration validity) {
        send(account, new PasswordResetMail(account.username(), new AppPath("/reset-password#token=" + rawToken),
            validity.toMinutes()), expiresAt);
    }

    @Override
    public void passwordChanged(AccountContact account) {
        send(account, new PasswordChangedMail(account.username(), LOGIN),
            clock.instant().plus(ALERT_VALIDITY));
    }

    @Override
    public void accountInvitation(AccountContact account, String rawToken, Instant expiresAt, String inviterName) {
        Objects.requireNonNull(inviterName, "An invitation names who sends it");
        send(account, new AccountInvitationMail(account.username(), inviterName, welcome(rawToken), VALIDITY_DAYS),
            expiresAt);
    }

    @Override
    public void invitationRenewed(AccountContact account, String rawToken, Instant expiresAt) {
        send(account, new InvitationRenewedMail(account.username(), welcome(rawToken), VALIDITY_DAYS), expiresAt);
    }

    private static AppPath welcome(String rawToken) {
        return new AppPath(AccountInvitationRules.welcomePath(rawToken));
    }

    private void send(AccountContact account, MailContent content, Instant expiresAt) {
        if (account.email() == null || account.email().isBlank()) {
            log.warn("Account {} has no address: {} not sent", account.userId(), content.template());
            return;
        }
        sendMail.send(new MailRequest(new Recipient(account.email(), account.username()),
            MailLanguage.resolve(account.language()), content, expiresAt));
    }
}
