package com.nido.api.authentication.infrastructure.mail;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;

/** mail/authentication/account-invitation.html — the first mail of an account an administrator created. */
public record AccountInvitationMail(String username, String inviterName, AppPath welcomePath, long validityDays)
    implements MailContent {

    @Override
    public String template() {
        return "authentication/account-invitation";
    }
}
