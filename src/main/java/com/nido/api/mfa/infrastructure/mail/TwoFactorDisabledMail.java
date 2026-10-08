package com.nido.api.mfa.infrastructure.mail;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.shared.model.TwoFactorMethod;

/** mail/mfa/two-factor-disabled.html */
public record TwoFactorDisabledMail(String username, TwoFactorMethod method, boolean anotherRemains, AppPath securityPath)
    implements MailContent {
    @Override
    public String template() {
        return "mfa/two-factor-disabled";
    }
}
