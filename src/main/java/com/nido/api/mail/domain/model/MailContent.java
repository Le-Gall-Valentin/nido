package com.nido.api.mail.domain.model;

/**
 * One kind of mail, as the context that sends it sees it: a public record whose components are the
 * data its template needs, exposed to the template as {@code mail}.
 *
 * <p>{@link #template()} names the template under {@code src/main/resources/mail/}, without
 * {@code .html}: {@code "authentication/password-reset"}. The template, its
 * {@code _fr}/{@code _en} messages and this record belong to the sending context; the mail context
 * never learns what the mail is about.
 */
public interface MailContent {
    String template();
}
