package com.nido.api.notifications.domain.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.nido.api.notifications.domain.model.NotificationChannel.EMAIL;
import static org.assertj.core.api.Assertions.assertThat;

class NotificationPreferencesTest {

    private static final NotificationType INVITATION = new NotificationType("space.invitation");
    private static final NotificationType REMINDER = new NotificationType("agenda.reminder");

    @Test
    void nothing_chosen_means_everything_on() {
        NotificationPreferences preferences = NotificationPreferences.DEFAULTS;

        assertThat(preferences.isOn(EMAIL)).isTrue();
        assertThat(preferences.isOn(INVITATION)).isTrue();
        assertThat(preferences.allows(INVITATION, EMAIL)).isTrue();
    }

    @Test
    void a_channel_switched_off_stops_every_kind_on_it() {
        NotificationPreferences preferences = new NotificationPreferences(Map.of(EMAIL, false), Map.of());

        assertThat(preferences.allows(INVITATION, EMAIL)).isFalse();
        assertThat(preferences.allows(REMINDER, EMAIL)).isFalse();
    }

    @Test
    void a_kind_switched_off_stops_only_that_kind() {
        NotificationPreferences preferences = new NotificationPreferences(Map.of(), Map.of(INVITATION, false));

        assertThat(preferences.allows(INVITATION, EMAIL)).isFalse();
        assertThat(preferences.allows(REMINDER, EMAIL)).isTrue();
    }

    @Test
    void a_channel_switched_back_on_finds_each_kind_as_it_was() {
        NotificationPreferences preferences = new NotificationPreferences(Map.of(EMAIL, true), Map.of(INVITATION, false));

        assertThat(preferences.allows(INVITATION, EMAIL)).isFalse();
        assertThat(preferences.allows(REMINDER, EMAIL)).isTrue();
    }

    @Test
    void what_it_was_built_from_cannot_change_it_afterwards() {
        Map<NotificationType, Boolean> types = new HashMap<>(Map.of(INVITATION, true));
        NotificationPreferences preferences = new NotificationPreferences(Map.of(), types);

        types.put(INVITATION, false);

        assertThat(preferences.isOn(INVITATION)).isTrue();
    }
}
