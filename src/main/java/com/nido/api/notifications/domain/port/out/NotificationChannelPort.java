package com.nido.api.notifications.domain.port.out;

import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationChannel;
import com.nido.api.notifications.domain.model.NotificationRecipient;
import com.nido.api.notifications.domain.model.NotificationType;

import java.time.Instant;

/** One way to reach someone. Adding a channel is adding an implementation: nothing else asks which exist. */
public interface NotificationChannelPort {

    NotificationChannel channel();

    /** Whether this installation can deliver on the channel at all — mail: configured, from the environment or the settings page. */
    boolean isAvailable();

    /**
     * Whether notifications of this class can be written for the channel: they implement its content
     * type. Never depends on the configuration, so the startup check holds with the channel off too.
     */
    boolean supports(Class<? extends Notification> notificationClass);

    /** Whether a notification of this class can go out on this channel, here and now. */
    default boolean canDeliver(Class<? extends Notification> notificationClass) {
        return isAvailable() && supports(notificationClass);
    }

    /**
     * Hands the notification over for delivery, in the current transaction. Only called when the channel is
     * available, supports the notification and the account keeps it on. {@code type} names it in logs.
     */
    void deliver(NotificationRecipient recipient, NotificationType type, Notification notification, Instant expiresAt);
}
