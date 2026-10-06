package com.nido.api.identity.infrastructure.mail;

import com.nido.api.mail.domain.model.MailContent;

/** mail/identity/account-deactivated.html */
public record AccountDeactivatedMail(String username, String actorName) implements MailContent {
    @Override
    public String template() {
        return "identity/account-deactivated";
    }
}
