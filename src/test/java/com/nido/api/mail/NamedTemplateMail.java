package com.nido.api.mail;

import com.nido.api.mail.domain.model.MailContent;

/** A test-only mail whose template is chosen by the test — for the templates that must fail. */
public record NamedTemplateMail(String template) implements MailContent {
}
