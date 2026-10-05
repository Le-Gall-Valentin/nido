package com.nido.api.authentication.infrastructure.mail;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;

/** mail/authentication/invitation-renewed.html — a new invitation link, asked for from "forgot password". */
public record InvitationRenewedMail(String username, AppPath welcomePath, long validityDays) implements MailContent {

    @Override
    public String template() {
        return "authentication/invitation-renewed";
    }
}
