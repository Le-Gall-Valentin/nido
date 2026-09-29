package com.nido.api.authentication.infrastructure.mail;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;

/** mail/authentication/password-changed.html */
public record PasswordChangedMail(String username, AppPath loginPath) implements MailContent {
    @Override
    public String template() {
        return "authentication/password-changed";
    }
}
