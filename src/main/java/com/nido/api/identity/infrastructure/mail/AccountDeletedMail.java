package com.nido.api.identity.infrastructure.mail;

import com.nido.api.mail.domain.model.MailContent;

/** mail/identity/account-deleted.html — no link: there is nothing left to open. */
public record AccountDeletedMail(String username, String actorName) implements MailContent {
    @Override
    public String template() {
        return "identity/account-deleted";
    }
}
