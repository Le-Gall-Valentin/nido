package com.nido.api.mail.domain.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * What a context hands to {@code SendMailUseCase}.
 *
 * @param locale    the language to write in; anything but French or English is written in English
 * @param expiresAt when the mail stops being worth sending (a reset mail dies with its link);
 *                  {@code null} means "whenever it can be delivered"
 */
public record MailRequest(Recipient to, Locale locale, MailContent content, Instant expiresAt) {

    public MailRequest {
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(locale, "locale");
        Objects.requireNonNull(content, "content");
    }

    public static MailRequest of(Recipient to, Locale locale, MailContent content) {
        return new MailRequest(to, locale, content, null);
    }
}
