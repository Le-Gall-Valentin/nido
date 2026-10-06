package com.nido.api.identity.infrastructure.mail;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;

/** mail/identity/totp-reset.html — sent only when the account's 2FA was actually on. */
public record TotpResetMail(String username, String actorName, AppPath securityPath) implements MailContent {
    @Override
    public String template() {
        return "identity/totp-reset";
    }
}
