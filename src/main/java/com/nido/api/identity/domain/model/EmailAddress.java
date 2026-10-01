package com.nido.api.identity.domain.model;

import java.util.Locale;
import java.util.Objects;

/**
 * An email address as Nido keeps and compares it: without surrounding spaces, lower-case. One mailbox,
 * one account — the database refuses anything else ({@code ck_users_email_lowercase}, and the unique index
 * on the address). Its format is checked where it is typed, by {@code @Email}.
 */
public record EmailAddress(String value) {

    public EmailAddress {
        Objects.requireNonNull(value, "email");
        value = value.strip().toLowerCase(Locale.ROOT);
    }

    /** The address as kept, or null when there is none — commands receive what the request carried. */
    public static String normalize(String raw) {
        return raw == null ? null : new EmailAddress(raw).value();
    }
}
