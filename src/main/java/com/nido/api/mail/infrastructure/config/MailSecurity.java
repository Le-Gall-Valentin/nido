package com.nido.api.mail.infrastructure.config;

import java.util.Locale;
import java.util.Optional;

/** How the connection to the SMTP server is protected. */
public enum MailSecurity {
    /** Port 587: plain connection upgraded to TLS, the upgrade being required. */
    STARTTLS,
    /** Port 465: TLS from the first byte. */
    TLS,
    /** No encryption at all — local development against Mailpit only. */
    NONE;

    /** Blank means the default, STARTTLS; anything unknown is empty. */
    public static Optional<MailSecurity> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.of(STARTTLS);
        }
        try {
            return Optional.of(valueOf(value.strip().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
