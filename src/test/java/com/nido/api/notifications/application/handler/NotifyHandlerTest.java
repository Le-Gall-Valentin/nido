package com.nido.api.notifications.application.handler;

import com.nido.api.notifications.domain.model.NotificationCatalog;
import com.nido.api.notifications.domain.model.NotificationChannel;
import com.nido.api.notifications.domain.model.NotificationPreferences;
import com.nido.api.notifications.domain.model.NotificationRecipient;
import com.nido.api.notifications.domain.model.NotificationRequest;
import com.nido.api.notifications.domain.model.NotificationType;
import com.nido.api.notifications.domain.port.out.NotificationChannelPort;
import com.nido.api.notifications.domain.port.out.NotificationPreferencesRepository;
import com.nido.api.notifications.domain.port.out.NotificationRecipientPort;
import com.nido.api.shared.model.Language;
import fixtures.notifications.valid.GreetingNotification;
import fixtures.notifications.valid.ReminderNotification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class NotifyHandlerTest {

    private static final NotificationType GREETING = new NotificationType("fixture.greeting");
    private static final NotificationCatalog CATALOG = new NotificationCatalog(Map.of(GreetingNotification.class, GREETING));
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-09T10:00:00Z");

    @Mock NotificationRecipientPort recipients;
    @Mock NotificationPreferencesRepository preferences;

    private final UUID janeId = UUID.randomUUID();
    private final NotificationRecipient jane = new NotificationRecipient(janeId, "jane", "jane@test.local", Language.FR, true);
    private final GreetingNotification greeting = new GreetingNotification("jane");

    private NotifyHandler handler(NotificationChannelPort... channels) {
        return new NotifyHandler(() -> CATALOG, recipients, preferences, List.of(channels));
    }

    /** A mail channel; lenient, since a test that stops early never asks it everything. */
    private static NotificationChannelPort channel(boolean available, boolean supports) {
        NotificationChannelPort channel = mock(NotificationChannelPort.class, withSettings().strictness(Strictness.LENIENT));
        when(channel.channel()).thenReturn(NotificationChannel.EMAIL);
        when(channel.isAvailable()).thenReturn(available);
        when(channel.supports(GreetingNotification.class)).thenReturn(supports);
        return channel;
    }

    private void janeChose(NotificationPreferences chosen) {
        when(recipients.find(janeId)).thenReturn(Optional.of(jane));
        when(preferences.find(janeId)).thenReturn(chosen);
    }

    private NotificationRequest request() {
        return new NotificationRequest(janeId, greeting, EXPIRES_AT);
    }

    @Test
    void an_open_channel_delivers_it_with_its_expiry() {
        NotificationChannelPort mail = channel(true, true);
        janeChose(NotificationPreferences.DEFAULTS);

        handler(mail).notify(request());

        verify(mail).deliver(jane, GREETING, greeting, EXPIRES_AT);
    }

    @Test
    void an_account_that_does_not_exist_is_told_nothing() {
        NotificationChannelPort mail = channel(true, true);
        when(recipients.find(janeId)).thenReturn(Optional.empty());

        handler(mail).notify(request());

        verify(mail, never()).deliver(any(), any(), any(), any());
        verifyNoInteractions(preferences);
    }

    @Test
    void a_deactivated_account_is_told_nothing() {
        NotificationChannelPort mail = channel(true, true);
        when(recipients.find(janeId))
            .thenReturn(Optional.of(new NotificationRecipient(janeId, "jane", "jane@test.local", Language.FR, false)));

        handler(mail).notify(request());

        verify(mail, never()).deliver(any(), any(), any(), any());
        verifyNoInteractions(preferences);
    }

    @Test
    void without_a_channel_on_this_installation_nobody_is_even_looked_up() {
        NotificationChannelPort mail = channel(false, true);

        handler(mail).notify(request());

        verify(mail, never()).deliver(any(), any(), any(), any());
        verifyNoInteractions(recipients, preferences);
    }

    @Test
    void a_channel_that_cannot_write_it_is_passed_over() {
        NotificationChannelPort mail = channel(true, false);

        handler(mail).notify(request());

        verify(mail, never()).deliver(any(), any(), any(), any());
        verifyNoInteractions(recipients, preferences);
    }

    @Test
    void a_channel_the_account_switched_off_is_passed_over() {
        NotificationChannelPort mail = channel(true, true);
        janeChose(new NotificationPreferences(Map.of(NotificationChannel.EMAIL, false), Map.of()));

        handler(mail).notify(request());

        verify(mail, never()).deliver(any(), any(), any(), any());
    }

    @Test
    void a_kind_the_account_switched_off_is_passed_over() {
        NotificationChannelPort mail = channel(true, true);
        janeChose(new NotificationPreferences(Map.of(), Map.of(GREETING, false)));

        handler(mail).notify(request());

        verify(mail, never()).deliver(any(), any(), any(), any());
    }

    @Test
    void only_the_channels_that_can_deliver_are_used() {
        NotificationChannelPort open = channel(true, true);
        NotificationChannelPort missing = channel(false, true);
        janeChose(NotificationPreferences.DEFAULTS);

        handler(missing, open).notify(request());

        verify(open).deliver(jane, GREETING, greeting, EXPIRES_AT);
        verify(missing, never()).deliver(any(), any(), any(), any());
    }

    @Test
    void an_uncatalogued_notification_is_a_bug_not_a_silence() {
        NotificationChannelPort mail = channel(true, true);

        assertThatThrownBy(() -> handler(mail).notify(new NotificationRequest(janeId, new ReminderNotification("x"), null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(ReminderNotification.class.getName());
        verifyNoInteractions(recipients, preferences);
    }
}
