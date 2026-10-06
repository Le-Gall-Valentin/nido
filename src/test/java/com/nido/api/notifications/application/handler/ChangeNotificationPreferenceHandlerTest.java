package com.nido.api.notifications.application.handler;

import com.nido.api.notifications.domain.model.NotificationCatalog;
import com.nido.api.notifications.domain.model.NotificationChannel;
import com.nido.api.notifications.domain.model.NotificationException;
import com.nido.api.notifications.domain.model.NotificationType;
import com.nido.api.notifications.domain.port.out.NotificationChannelPort;
import com.nido.api.notifications.domain.port.out.NotificationPreferencesRepository;
import com.nido.api.shared.model.Role;
import fixtures.notifications.valid.GreetingNotification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ChangeNotificationPreferenceHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-02T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final NotificationType GREETING = new NotificationType("fixture.greeting");
    private static final NotificationCatalog CATALOG = new NotificationCatalog(Map.of(GreetingNotification.class, GREETING));

    @Mock NotificationPreferencesRepository preferences;

    private final UUID janeId = UUID.randomUUID();

    private ChangeNotificationPreferenceHandler handler(boolean mailAvailable) {
        NotificationChannelPort mail = mock(NotificationChannelPort.class);
        lenient().when(mail.channel()).thenReturn(NotificationChannel.EMAIL);
        lenient().when(mail.isAvailable()).thenReturn(mailAvailable);
        return new ChangeNotificationPreferenceHandler(() -> CATALOG, preferences, List.of(mail), CLOCK);
    }

    @Test
    void a_channel_of_this_installation_is_saved_with_the_time() {
        handler(true).changeChannel(janeId, "email", false);

        verify(preferences).saveChannel(janeId, NotificationChannel.EMAIL, false, NOW);
    }

    @Test
    void a_channel_this_installation_lacks_is_unknown() {
        assertThatThrownBy(() -> handler(false).changeChannel(janeId, "email", false))
            .isInstanceOf(NotificationException.UnknownChannel.class);
        verifyNoInteractions(preferences);
    }

    @Test
    void a_code_that_names_no_channel_is_unknown() {
        assertThatThrownBy(() -> handler(true).changeChannel(janeId, "sms", false))
            .isInstanceOf(NotificationException.UnknownChannel.class);
        verifyNoInteractions(preferences);
    }

    @Test
    void a_catalogued_kind_is_saved_with_the_time() {
        handler(true).changeType(janeId, Role.USER, "fixture.greeting", false);

        verify(preferences).saveType(janeId, GREETING, false, NOW);
    }

    @Test
    void a_kind_reserved_to_another_role_is_unknown_to_this_account() {
        NotificationCatalog reserved = new NotificationCatalog(Map.of(GreetingNotification.class, GREETING),
            Map.of(GREETING, Set.of(Role.SUPER_ADMIN)));
        ChangeNotificationPreferenceHandler handler =
            new ChangeNotificationPreferenceHandler(() -> reserved, preferences, List.of(), CLOCK);

        assertThatThrownBy(() -> handler.changeType(janeId, Role.ADMIN, "fixture.greeting", false))
            .isInstanceOf(NotificationException.UnknownType.class);
        verifyNoInteractions(preferences);

        handler.changeType(janeId, Role.SUPER_ADMIN, "fixture.greeting", false);
        verify(preferences).saveType(janeId, GREETING, false, NOW);
    }

    @Test
    void a_kind_the_catalogue_does_not_know_is_unknown_even_when_malformed() {
        assertThatThrownBy(() -> handler(true).changeType(janeId, Role.USER, "fixture.unknown", true))
            .isInstanceOf(NotificationException.UnknownType.class);
        assertThatThrownBy(() -> handler(true).changeType(janeId, Role.USER, "Not A Code", true))
            .isInstanceOf(NotificationException.UnknownType.class);
        verifyNoInteractions(preferences);
    }
}
