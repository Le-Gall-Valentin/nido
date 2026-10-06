package com.nido.api.mail.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

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

    /** How long an alert about an account stays worth sending: one queued while mail was off must not arrive weeks late. */
    public static final Duration ALERT_VALIDITY = Duration.ofHours(24);

    public static MailRequest of(Recipient to, Locale locale, MailContent content) {
        return new MailRequest(to, locale, content, null);
    }

    /**
     * A mail to an account, under its name; empty when the account has no address — an anonymised one, or
     * one created without — for the caller to say so in its own words.
     */
    public static Optional<MailRequest> forAccount(String address, String name, Locale locale, MailContent content,
                                                   Instant expiresAt) {
        if (address == null || address.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new MailRequest(new Recipient(address, name), locale, content, expiresAt));
    }
}
