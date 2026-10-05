package com.nido.api.notifications.domain.model;

import com.nido.api.shared.model.Language;
import com.nido.api.shared.model.Role;

import java.util.Objects;
import java.util.UUID;

/**
 * Who a notification is for, as the channels need them. {@code email} and {@code language} may be
 * missing; a channel that needs one and finds none delivers nothing.
 */
public record NotificationRecipient(UUID userId, String username, String email, Language language, Role role,
                                    boolean active) {

    public NotificationRecipient {
        Objects.requireNonNull(userId, "userId");
    }

    /** This account, written to in {@code fallback} when it has no language of its own. */
    public NotificationRecipient withLanguageOr(Language fallback) {
        return language != null || fallback == null
            ? this
            : new NotificationRecipient(userId, username, email, fallback, role, active);
    }
}
