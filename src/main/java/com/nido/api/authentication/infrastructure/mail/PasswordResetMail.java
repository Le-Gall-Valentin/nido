package com.nido.api.authentication.infrastructure.mail;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;

/** mail/authentication/password-reset.html */
public record PasswordResetMail(String username, AppPath resetPath, long validityMinutes) implements MailContent {
    @Override
    public String template() {
        return "authentication/password-reset";
    }
}
