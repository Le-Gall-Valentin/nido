package com.nido.api.identity.infrastructure.mail;

import com.nido.api.mail.domain.model.MailContent;

/** mail/identity/invitation-cancelled.html — for an account deleted before it chose its password. */
public record AccountInvitationCancelledMail(String username, String actorName) implements MailContent {
    @Override
    public String template() {
        return "identity/invitation-cancelled";
    }
}
