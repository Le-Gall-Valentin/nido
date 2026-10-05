package com.nido.api.identity.infrastructure.mail;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;

/** mail/identity/role-changed.html — {@code nowAdmin}: an administrator now, rather than back to a user. */
public record RoleChangedMail(String username, String actorName, boolean nowAdmin, AppPath loginPath) implements MailContent {
    @Override
    public String template() {
        return "identity/role-changed";
    }
}
