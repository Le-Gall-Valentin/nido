package com.nido.api.authentication.infrastructure.mail;

import com.nido.api.authentication.domain.model.AccountContact;
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

import java.time.Duration;
import java.time.Instant;

/**
 * Authentication's account mails, sent through the mail context. The reset link carries its token
 * after the '#': that part never reaches a server, so the token appears in no access log and no
 * Referer header.
 */
@Component
public class AccountMailAdapter implements AccountMailPort {

    private static final Logger log = LoggerFactory.getLogger(AccountMailAdapter.class);
    private static final AppPath LOGIN = new AppPath("/login");

    private final SendMailUseCase sendMail;
    private final MailAvailabilityQuery availability;

    public AccountMailAdapter(SendMailUseCase sendMail, MailAvailabilityQuery availability) {
        this.sendMail = sendMail;
        this.availability = availability;
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
        send(account, new PasswordChangedMail(account.username(), LOGIN), null);
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
