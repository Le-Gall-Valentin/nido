package com.nido.api.notifications.domain.model;

import java.util.Map;

/**
 * What an account chose. An entry that is absent is on: every channel and every kind start on, the ones
 * added after the account was created included. Channels and kinds are two independent levels — switching
 * a channel off leaves each kind's choice as it was, for when the channel comes back on.
 */
public record NotificationPreferences(Map<NotificationChannel, Boolean> channels, Map<NotificationType, Boolean> types) {

    public static final NotificationPreferences DEFAULTS = new NotificationPreferences(Map.of(), Map.of());

    public NotificationPreferences {
        channels = Map.copyOf(channels);
        types = Map.copyOf(types);
    }

    public boolean isOn(NotificationChannel channel) {
        return channels.getOrDefault(channel, true);
    }

    public boolean isOn(NotificationType type) {
        return types.getOrDefault(type, true);
    }

    /**
     * The one rule: a kind the account keeps on, on a channel it keeps on. Whether the installation has
     * the channel at all is not a preference, and is not asked here.
     */
    public boolean allows(NotificationType type, NotificationChannel channel) {
        return isOn(channel) && isOn(type);
    }
}
