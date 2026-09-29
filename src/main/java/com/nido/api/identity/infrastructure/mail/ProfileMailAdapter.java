package com.nido.api.identity.infrastructure.mail;

import com.nido.api.identity.domain.model.Language;
import com.nido.api.identity.domain.port.out.ProfileMailPort;
import com.nido.api.infrastructure.web.MailLanguage;
import com.nido.api.mail.application.port.in.SendMailUseCase;
import com.nido.api.mail.domain.model.MailRequest;
import com.nido.api.mail.domain.model.Recipient;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

@Component
public class ProfileMailAdapter implements ProfileMailPort {

    /** An alert that outlives its moment is worse than none: a queue kept while mail was off must not deliver it weeks late. */
    private static final Duration ALERT_VALIDITY = Duration.ofHours(24);

    private final SendMailUseCase sendMail;
    private final Clock clock;

    public ProfileMailAdapter(SendMailUseCase sendMail, Clock clock) {
        this.sendMail = sendMail;
        this.clock = clock;
    }

    @Override
    public void emailChanged(String username, String previousEmail, String newEmail, Language language) {
        sendMail.send(new MailRequest(new Recipient(previousEmail, username),
            MailLanguage.resolve(language == null ? null : language.code()),
            new EmailChangedMail(username, EmailChangedMail.mask(newEmail)),
            clock.instant().plus(ALERT_VALIDITY)));
    }
}
