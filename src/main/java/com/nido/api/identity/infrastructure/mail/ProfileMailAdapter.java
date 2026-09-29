package com.nido.api.identity.infrastructure.mail;

import com.nido.api.identity.domain.model.Language;
import com.nido.api.identity.domain.port.out.ProfileMailPort;
import com.nido.api.infrastructure.web.MailLanguage;
import com.nido.api.mail.application.port.in.SendMailUseCase;
import com.nido.api.mail.domain.model.MailRequest;
import com.nido.api.mail.domain.model.Recipient;
import org.springframework.stereotype.Component;

@Component
public class ProfileMailAdapter implements ProfileMailPort {

    private final SendMailUseCase sendMail;

    public ProfileMailAdapter(SendMailUseCase sendMail) {
        this.sendMail = sendMail;
    }

    @Override
    public void emailChanged(String username, String previousEmail, String newEmail, Language language) {
        sendMail.send(MailRequest.of(new Recipient(previousEmail, username),
            MailLanguage.resolve(language == null ? null : language.code()),
            new EmailChangedMail(username, EmailChangedMail.mask(newEmail))));
    }
}
