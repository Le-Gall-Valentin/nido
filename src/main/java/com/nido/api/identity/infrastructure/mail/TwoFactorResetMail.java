package com.nido.api.identity.infrastructure.mail;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;

/** mail/identity/two-factor-reset.html — sent only when a method was actually removed. */
public record TwoFactorResetMail(String username, String actorName, ResetMethods removed, KeptMethod kept,
                                 AppPath securityPath) implements MailContent {
    @Override
    public String template() {
        return "identity/two-factor-reset";
    }
}
