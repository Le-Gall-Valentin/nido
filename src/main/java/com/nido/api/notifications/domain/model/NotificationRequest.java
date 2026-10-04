package com.nido.api.notifications.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * What a context hands to {@code NotifyUseCase}.
 *
 * @param expiresAt when the notification stops being worth delivering (an invitation mail dies with the
 *                  invitation); {@code null} means whenever it can be delivered
 */
public record NotificationRequest(UUID recipientId, Notification notification, Instant expiresAt) {

    public NotificationRequest {
        Objects.requireNonNull(recipientId, "recipientId");
        Objects.requireNonNull(notification, "notification");
    }
}
