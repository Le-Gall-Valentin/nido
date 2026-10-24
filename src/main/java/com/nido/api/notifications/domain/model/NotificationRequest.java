package com.nido.api.notifications.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * What a context hands to {@code NotifyUseCase}.
 *
 * @param expiresAt when the notification stops being worth delivering (an invitation mail dies with the
 *                  invitation); {@code null} means whenever it can be delivered
 * @param fallback  delivered instead, on a channel where the account switched the main kind off but keeps this
 *                  one's; {@code null} when there is none
 */
public record NotificationRequest(UUID recipientId, Notification notification, Instant expiresAt, Notification fallback) {

    public NotificationRequest {
        Objects.requireNonNull(recipientId, "recipientId");
        Objects.requireNonNull(notification, "notification");
    }

    public NotificationRequest(UUID recipientId, Notification notification, Instant expiresAt) {
        this(recipientId, notification, expiresAt, null);
    }
}
