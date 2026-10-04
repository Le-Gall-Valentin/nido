package com.nido.api.notifications.domain.port.out;

import com.nido.api.notifications.domain.model.NotificationChannel;
import com.nido.api.notifications.domain.model.NotificationPreferences;
import com.nido.api.notifications.domain.model.NotificationType;

import java.time.Instant;
import java.util.UUID;

public interface NotificationPreferencesRepository {

    /** What the account chose; {@link NotificationPreferences#DEFAULTS} when it never chose anything. */
    NotificationPreferences find(UUID userId);

    /** Records the choice, replacing an earlier one for the same channel. */
    void saveChannel(UUID userId, NotificationChannel channel, boolean enabled, Instant now);

    /** Records the choice, replacing an earlier one for the same kind. */
    void saveType(UUID userId, NotificationType type, boolean enabled, Instant now);

    void deleteAllFor(UUID userId);
}
