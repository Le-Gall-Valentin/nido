package com.nido.api.identity.infrastructure.mail;

import com.nido.api.identity.domain.port.out.ProfileMailPort;
import com.nido.api.infrastructure.web.MailLanguage;
import com.nido.api.mail.application.port.in.SendMailUseCase;
import com.nido.api.mail.domain.model.MailRequest;
import com.nido.api.mail.domain.model.Recipient;
import com.nido.api.shared.model.Language;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class ProfileMailAdapter implements ProfileMailPort {


    private final SendMailUseCase sendMail;
    private final Clock clock;

    public ProfileMailAdapter(SendMailUseCase sendMail, Clock clock) {
        this.sendMail = sendMail;
        this.clock = clock;
    }

    @Override
    public void emailChanged(String username, String previousEmail, String newEmail, Language language) {
        sendMail.send(new MailRequest(new Recipient(previousEmail, username),
            MailLanguage.resolve(language),
            new EmailChangedMail(username, EmailChangedMail.mask(newEmail)),
            clock.instant().plus(MailRequest.ALERT_VALIDITY)));
    }
}
