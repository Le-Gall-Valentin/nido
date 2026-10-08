package com.nido.api.mfa.infrastructure.mail;

import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.shared.model.TwoFactorMethod;

/** mail/mfa/two-factor-enabled.html — no link: whoever turned it on is already where it is turned off. */
public record TwoFactorEnabledMail(String username, TwoFactorMethod method) implements MailContent {
    @Override
    public String template() {
        return "mfa/two-factor-enabled";
    }
}
