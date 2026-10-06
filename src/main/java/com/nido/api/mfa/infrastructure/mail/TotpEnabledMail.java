package com.nido.api.mfa.infrastructure.mail;

import com.nido.api.mail.domain.model.MailContent;

/** mail/mfa/totp-enabled.html — no link: whoever turned it on is already where it is turned off. */
public record TotpEnabledMail(String username) implements MailContent {
    @Override
    public String template() {
        return "mfa/totp-enabled";
    }
}
