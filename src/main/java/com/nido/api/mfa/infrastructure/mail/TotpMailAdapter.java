package com.nido.api.mfa.infrastructure.mail;

import com.nido.api.identity.application.port.in.FindUserUseCase;
import com.nido.api.identity.domain.model.User;
import com.nido.api.infrastructure.web.MailLanguage;
import com.nido.api.mail.application.port.in.SendMailUseCase;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.mail.domain.model.MailRequest;
import com.nido.api.mfa.domain.port.out.TotpMailPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;
import java.util.function.Function;

/**
 * mfa's mails, through the mail context. mfa knows an account by its id only: the address, the name and the
 * language are identity's, read when the mail is queued.
 */
@Component
public class TotpMailAdapter implements TotpMailPort {

    private static final Logger log = LoggerFactory.getLogger(TotpMailAdapter.class);

    private static final AppPath SECURITY = new AppPath("/account/security");

    private final FindUserUseCase findUser;
    private final SendMailUseCase sendMail;
    private final Clock clock;

    public TotpMailAdapter(FindUserUseCase findUser, SendMailUseCase sendMail, Clock clock) {
        this.findUser = findUser;
        this.sendMail = sendMail;
        this.clock = clock;
    }

    @Override
    public void totpEnabled(UUID userId) {
        send(userId, user -> new TotpEnabledMail(user.username()));
    }

    @Override
    public void totpDisabled(UUID userId) {
        send(userId, user -> new TotpDisabledMail(user.username(), SECURITY));
    }

    private void send(UUID userId, Function<User, MailContent> content) {
        findUser.findById(userId)
            .flatMap(user -> MailRequest.forAccount(user.email(), user.username(), MailLanguage.resolve(user.language()),
                content.apply(user), clock.instant().plus(MailRequest.ALERT_VALIDITY)))
            .ifPresentOrElse(sendMail::send,
                () -> log.warn("Account {} cannot be written to: its 2FA mail is not sent", userId));
    }
}
