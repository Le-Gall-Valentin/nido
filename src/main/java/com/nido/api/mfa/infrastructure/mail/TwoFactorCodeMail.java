package com.nido.api.mfa.infrastructure.mail;

import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.mfa.domain.model.CodePurpose;

/** mail/mfa/two-factor-code.html — the code is in the body, never in the subject (lock screens, previews). */
public record TwoFactorCodeMail(String username, String code, CodePurpose purpose) implements MailContent {
    @Override
    public String template() {
        return "mfa/two-factor-code";
    }
}
