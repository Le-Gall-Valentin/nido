package com.nido.api.identity.infrastructure.mail;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;

/** mail/identity/account-reactivated.html */
public record AccountReactivatedMail(String username, String actorName, AppPath loginPath) implements MailContent {
    @Override
    public String template() {
        return "identity/account-reactivated";
    }
}
