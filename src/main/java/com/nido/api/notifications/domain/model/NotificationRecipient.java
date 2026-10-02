package com.nido.api.notifications.domain.model;

import com.nido.api.shared.model.Language;

import java.util.Objects;
import java.util.UUID;

/**
 * Who a notification is for, as the channels need them. {@code email} and {@code language} may be
 * missing; a channel that needs one and finds none delivers nothing.
 */
public record NotificationRecipient(UUID userId, String username, String email, Language language, boolean active) {

    public NotificationRecipient {
        Objects.requireNonNull(userId, "userId");
    }
}
