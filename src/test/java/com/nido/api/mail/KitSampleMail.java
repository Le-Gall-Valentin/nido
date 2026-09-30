package com.nido.api.mail;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;

/** A test-only mail that uses every component of the kit (src/test/resources/mail/test/kit-sample.html). */
public record KitSampleMail(String username, AppPath link) implements MailContent {
    @Override
    public String template() {
        return "test/kit-sample";
    }
}
