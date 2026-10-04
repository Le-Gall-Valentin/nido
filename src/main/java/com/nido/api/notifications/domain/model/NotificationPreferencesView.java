package com.nido.api.notifications.domain.model;

import java.util.List;

/** What the preferences card shows: the channels this installation has, and every kind, each with its state. */
public record NotificationPreferencesView(List<ChannelSetting> channels, List<TypeSetting> types) {

    public NotificationPreferencesView {
        channels = List.copyOf(channels);
        types = List.copyOf(types);
    }

    public record ChannelSetting(NotificationChannel channel, boolean enabled) {}

    public record TypeSetting(NotificationType type, boolean enabled) {}
}
