package com.nido.api.notifications.application.handler;

import com.nido.api.notifications.domain.model.NotificationCatalog;
import com.nido.api.notifications.domain.model.NotificationChannel;
import com.nido.api.notifications.domain.model.NotificationPreferences;
import com.nido.api.notifications.domain.model.NotificationPreferencesView;
import com.nido.api.notifications.domain.model.NotificationPreferencesView.ChannelSetting;
import com.nido.api.notifications.domain.model.NotificationPreferencesView.TypeSetting;
import com.nido.api.notifications.domain.model.NotificationType;
import com.nido.api.notifications.domain.port.out.NotificationChannelPort;
import com.nido.api.notifications.domain.port.out.NotificationPreferencesRepository;
import com.nido.api.shared.model.Role;
import fixtures.notifications.valid.GreetingNotification;
import fixtures.notifications.valid.ReminderNotification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetNotificationPreferencesHandlerTest {

    private static final NotificationType GREETING = new NotificationType("fixture.greeting");
    private static final NotificationType REMINDER = new NotificationType("agenda.reminder");
    private static final NotificationCatalog CATALOG = new NotificationCatalog(Map.of(
        GreetingNotification.class, GREETING, ReminderNotification.class, REMINDER));

    @Mock NotificationPreferencesRepository preferences;

    private final UUID janeId = UUID.randomUUID();

    private static NotificationChannelPort mail(boolean available) {
        NotificationChannelPort channel = mock(NotificationChannelPort.class);
        when(channel.isAvailable()).thenReturn(available);
        if (available) {
            when(channel.channel()).thenReturn(NotificationChannel.EMAIL);
        }
        return channel;
    }

    private NotificationPreferencesView view(NotificationChannelPort channel) {
        return new GetNotificationPreferencesHandler(() -> CATALOG, preferences, List.of(channel)).get(janeId, Role.USER);
    }

    @Test
    void an_account_that_never_chose_sees_everything_on_in_catalogue_order() {
        when(preferences.find(janeId)).thenReturn(NotificationPreferences.DEFAULTS);

        NotificationPreferencesView view = view(mail(true));

        assertThat(view.channels()).containsExactly(new ChannelSetting(NotificationChannel.EMAIL, true));
        assertThat(view.types()).containsExactly(new TypeSetting(REMINDER, true), new TypeSetting(GREETING, true));
    }

    @Test
    void a_channel_this_installation_lacks_is_not_shown_but_every_kind_still_is() {
        when(preferences.find(janeId)).thenReturn(NotificationPreferences.DEFAULTS);

        NotificationPreferencesView view = view(mail(false));

        assertThat(view.channels()).isEmpty();
        assertThat(view.types()).hasSize(2);
    }

    @Test
    void what_the_account_switched_off_shows_off() {
        when(preferences.find(janeId)).thenReturn(new NotificationPreferences(
            Map.of(NotificationChannel.EMAIL, false), Map.of(GREETING, false)));

        NotificationPreferencesView view = view(mail(true));

        assertThat(view.channels()).containsExactly(new ChannelSetting(NotificationChannel.EMAIL, false));
        assertThat(view.types()).containsExactly(new TypeSetting(REMINDER, true), new TypeSetting(GREETING, false));
    }

    @Test
    void a_kind_reserved_to_another_role_is_not_on_the_card() {
        when(preferences.find(janeId)).thenReturn(NotificationPreferences.DEFAULTS);
        NotificationCatalog reserved = new NotificationCatalog(
            Map.of(GreetingNotification.class, GREETING, ReminderNotification.class, REMINDER),
            Map.of(GREETING, Set.of(Role.SUPER_ADMIN)));
        GetNotificationPreferencesHandler handler =
            new GetNotificationPreferencesHandler(() -> reserved, preferences, List.of(mail(true)));

        assertThat(handler.get(janeId, Role.USER).types()).extracting(TypeSetting::type).containsExactly(REMINDER);
        assertThat(handler.get(janeId, Role.SUPER_ADMIN).types()).extracting(TypeSetting::type)
            .containsExactly(REMINDER, GREETING);
    }

    @Test
    void a_choice_for_a_kind_that_no_longer_exists_is_not_shown() {
        when(preferences.find(janeId)).thenReturn(new NotificationPreferences(
            Map.of(), Map.of(new NotificationType("gone.kind"), false)));

        NotificationPreferencesView view = view(mail(true));

        assertThat(view.types()).extracting(setting -> setting.type().code())
            .containsExactly("agenda.reminder", "fixture.greeting");
    }
}
