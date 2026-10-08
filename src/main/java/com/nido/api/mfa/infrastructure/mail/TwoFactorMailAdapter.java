package com.nido.api.mfa.infrastructure.mail;

import com.nido.api.identity.application.port.in.FindUserUseCase;
import com.nido.api.identity.domain.model.User;
import com.nido.api.infrastructure.web.MailLanguage;
import com.nido.api.mail.application.port.in.SendMailUseCase;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.mail.domain.model.MailRequest;
import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.mfa.domain.port.out.TwoFactorMailPort;
import com.nido.api.shared.model.TwoFactorMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

/**
 * mfa's mails, through the mail context. mfa knows an account by its id only: the name and the language are
 * identity's, read when the mail is queued. Never logs a code.
 */
@Component
public class TwoFactorMailAdapter implements TwoFactorMailPort {

    private static final Logger log = LoggerFactory.getLogger(TwoFactorMailAdapter.class);
    private static final AppPath SECURITY = new AppPath("/account/security");

    private final FindUserUseCase findUser;
    private final SendMailUseCase sendMail;
    private final Clock clock;

    public TwoFactorMailAdapter(FindUserUseCase findUser, SendMailUseCase sendMail, Clock clock) {
        this.findUser = findUser;
        this.sendMail = sendMail;
        this.clock = clock;
    }

    @Override
    public void sendCode(UUID userId, String address, CodePurpose purpose, String code, Instant expiresAt) {
        findUser.findById(userId)
            .flatMap(user -> MailRequest.forAccount(address, user.username(), MailLanguage.resolve(user.language()),
                new TwoFactorCodeMail(user.username(), code, purpose), expiresAt))
            .ifPresentOrElse(sendMail::send,
                () -> log.warn("Account {} cannot be written to: its {} code is not sent", userId, purpose));
    }

    @Override
    public void methodEnabled(UUID userId, TwoFactorMethod method) {
        alert(userId, user -> new TwoFactorEnabledMail(user.username(), method));
    }

    @Override
    public void methodDisabled(UUID userId, TwoFactorMethod method, boolean anotherRemains) {
        alert(userId, user -> new TwoFactorDisabledMail(user.username(), method, anotherRemains, SECURITY));
    }

    /** An alert about the account: to its address, worth sending for a day (MailRequest.ALERT_VALIDITY). */
    private void alert(UUID userId, Function<User, MailContent> content) {
        findUser.findById(userId)
            .flatMap(user -> MailRequest.forAccount(user.email(), user.username(), MailLanguage.resolve(user.language()),
                content.apply(user), clock.instant().plus(MailRequest.ALERT_VALIDITY)))
            .ifPresentOrElse(sendMail::send,
                () -> log.warn("Account {} cannot be written to: its two-factor mail is not sent", userId));
    }
}
