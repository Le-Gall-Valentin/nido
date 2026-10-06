package com.nido.api.mfa.infrastructure.mail;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;

/** mail/mfa/totp-disabled.html */
public record TotpDisabledMail(String username, AppPath securityPath) implements MailContent {
    @Override
    public String template() {
        return "mfa/totp-disabled";
    }
}
