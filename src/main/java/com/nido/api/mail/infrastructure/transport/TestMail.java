package com.nido.api.mail.infrastructure.transport;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;

/** mail/mail/test.html — sent from the setup screen and the settings page, to see a configuration work. */
public record TestMail(AppPath homePath) implements MailContent {
    @Override
    public String template() {
        return "mail/test";
    }
}
