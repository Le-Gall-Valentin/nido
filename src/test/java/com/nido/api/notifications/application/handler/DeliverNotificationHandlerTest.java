package com.nido.api.notifications.application.handler;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class DeliverNotificationHandlerTest {

    private static final NotificationType GREETING = new NotificationType("fixture.greeting");
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-09T10:00:00Z");

    @Mock NotificationRecipientPort recipients;
    @Mock NotificationPreferencesRepository preferences;

    private final UUID janeId = UUID.randomUUID();
    private final NotificationRecipient jane = new NotificationRecipient(janeId, "jane", "jane@test.local", Language.FR, true);
    private final GreetingNotification greeting = new GreetingNotification("jane");

    private DeliverNotificationHandler handler(NotificationChannelPort... channels) {
        return new DeliverNotificationHandler(recipients, preferences, List.of(channels));
    }

    /** A mail channel; lenient, since a test that stops early never asks it everything. */
    private static NotificationChannelPort channel(boolean canDeliver) {
        NotificationChannelPort channel = mock(NotificationChannelPort.class, withSettings().strictness(Strictness.LENIENT));
        when(channel.channel()).thenReturn(NotificationChannel.EMAIL);
        when(channel.canDeliver(GreetingNotification.class)).thenReturn(canDeliver);
        return channel;
    }

    private void janeChose(NotificationPreferences chosen) {
        when(recipients.find(janeId)).thenReturn(Optional.of(jane));
        when(preferences.find(janeId)).thenReturn(chosen);
    }

    private void deliverTo(DeliverNotificationHandler handler) {
        handler.deliver(GREETING, new NotificationRequest(janeId, greeting, EXPIRES_AT), null);
    }

    @Test
    void it_runs_alone_in_its_transaction() throws Exception {
        Transactional transactional = DeliverNotificationHandler.class
            .getMethod("deliver", NotificationType.class, NotificationRequest.class, Language.class)
            .getAnnotation(Transactional.class);

        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }

    @Test
    void an_open_channel_delivers_it_with_its_kind_and_expiry() {
        NotificationChannelPort mail = channel(true);
        janeChose(NotificationPreferences.DEFAULTS);

        deliverTo(handler(mail));

        verify(mail).deliver(jane, GREETING, greeting, EXPIRES_AT);
    }

    @Test
    void an_account_that_does_not_exist_is_told_nothing() {
        NotificationChannelPort mail = channel(true);
        when(recipients.find(janeId)).thenReturn(Optional.empty());

        deliverTo(handler(mail));

        verify(mail, never()).deliver(any(), any(), any(), any());
        verifyNoInteractions(preferences);
    }

    @Test
    void a_deactivated_account_is_told_nothing() {
        NotificationChannelPort mail = channel(true);
        when(recipients.find(janeId))
            .thenReturn(Optional.of(new NotificationRecipient(janeId, "jane", "jane@test.local", Language.FR, false)));

        deliverTo(handler(mail));

        verify(mail, never()).deliver(any(), any(), any(), any());
        verifyNoInteractions(preferences);
    }

    @Test
    void without_a_channel_that_can_deliver_it_nobody_is_even_looked_up() {
        NotificationChannelPort mail = channel(false);

        deliverTo(handler(mail));

        verify(mail, never()).deliver(any(), any(), any(), any());
        verifyNoInteractions(recipients, preferences);
    }

    @Test
    void a_channel_the_account_switched_off_is_passed_over() {
        NotificationChannelPort mail = channel(true);
        janeChose(new NotificationPreferences(Map.of(NotificationChannel.EMAIL, false), Map.of()));

        deliverTo(handler(mail));

        verify(mail, never()).deliver(any(), any(), any(), any());
    }

    @Test
    void a_kind_the_account_switched_off_is_passed_over() {
        NotificationChannelPort mail = channel(true);
        janeChose(new NotificationPreferences(Map.of(), Map.of(GREETING, false)));

        deliverTo(handler(mail));

        verify(mail, never()).deliver(any(), any(), any(), any());
    }

    @Test
    void only_the_channels_that_can_deliver_are_used() {
        NotificationChannelPort open = channel(true);
        NotificationChannelPort closed = channel(false);
        janeChose(NotificationPreferences.DEFAULTS);

        deliverTo(handler(closed, open));

        verify(open).deliver(jane, GREETING, greeting, EXPIRES_AT);
        verify(closed, never()).deliver(any(), any(), any(), any());
    }

    @Test
    void an_account_without_a_language_is_written_to_in_the_language_of_whoever_acts() {
        NotificationChannelPort mail = channel(true);
        NotificationRecipient speechless = new NotificationRecipient(janeId, "jane", "jane@test.local", null, true);
        when(recipients.find(janeId)).thenReturn(Optional.of(speechless));
        when(preferences.find(janeId)).thenReturn(NotificationPreferences.DEFAULTS);

        handler(mail).deliver(GREETING, new NotificationRequest(janeId, greeting, EXPIRES_AT), Language.FR);

        verify(mail).deliver(new NotificationRecipient(janeId, "jane", "jane@test.local", Language.FR, true),
            GREETING, greeting, EXPIRES_AT);
    }

    @Test
    void an_account_s_own_language_wins_over_the_language_of_whoever_acts() {
        NotificationChannelPort mail = channel(true);
        janeChose(NotificationPreferences.DEFAULTS);

        handler(mail).deliver(GREETING, new NotificationRequest(janeId, greeting, EXPIRES_AT), Language.EN);

        verify(mail).deliver(jane, GREETING, greeting, EXPIRES_AT);
    }
}
