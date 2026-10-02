package com.nido.api.notifications.infrastructure.channel.mail;

import com.nido.api.mail.application.port.in.MailAvailabilityQuery;
import com.nido.api.mail.application.port.in.SendMailUseCase;
import com.nido.api.mail.domain.model.MailRequest;
import com.nido.api.mail.domain.model.Recipient;
import com.nido.api.notifications.domain.model.NotificationChannel;
import com.nido.api.notifications.domain.model.NotificationRecipient;
import com.nido.api.shared.model.Language;
import fixtures.notifications.nochannel.NoChannelNotification;
import fixtures.notifications.valid.GreetingNotification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailChannelAdapterTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-10-09T10:00:00Z");

    @Mock SendMailUseCase sendMail;
    @Mock MailAvailabilityQuery availability;

    private final UUID janeId = UUID.randomUUID();
    private final GreetingNotification greeting = new GreetingNotification("jane");

    private MailChannelAdapter adapter() {
        return new MailChannelAdapter(sendMail, availability);
    }

    private MailRequest sent() {
        ArgumentCaptor<MailRequest> request = ArgumentCaptor.forClass(MailRequest.class);
        verify(sendMail).send(request.capture());
        return request.getValue();
    }

    @Test
    void it_is_the_email_channel() {
        assertThat(adapter().channel()).isEqualTo(NotificationChannel.EMAIL);
    }

    @Test
    void it_is_available_exactly_when_mail_is_on() {
        when(availability.isAvailable()).thenReturn(true, false);

        assertThat(adapter().isAvailable()).isTrue();
        assertThat(adapter().isAvailable()).isFalse();
    }

    @Test
    void it_writes_what_is_written_as_a_mail_and_nothing_else() {
        assertThat(adapter().supports(GreetingNotification.class)).isTrue();
        assertThat(adapter().supports(NoChannelNotification.class)).isFalse();
    }

    @Test
    void the_notification_is_sent_as_the_mail_it_is_with_its_expiry() {
        adapter().deliver(new NotificationRecipient(janeId, "jane", "jane@test.local", Language.EN, true), greeting, EXPIRES_AT);

        MailRequest request = sent();
        assertThat(request.to()).isEqualTo(new Recipient("jane@test.local", "jane"));
        assertThat(request.locale()).isEqualTo(Locale.ENGLISH);
        assertThat(request.content()).isSameAs(greeting);
        assertThat(request.expiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    void it_is_written_in_the_account_language() {
        adapter().deliver(new NotificationRecipient(janeId, "jane", "jane@test.local", Language.FR, true), greeting, null);

        assertThat(sent().locale()).isEqualTo(Locale.FRENCH);
    }

    @Test
    void without_a_language_and_outside_a_request_it_is_written_in_english() {
        adapter().deliver(new NotificationRecipient(janeId, "jane", "jane@test.local", null, true), greeting, null);

        assertThat(sent().locale()).isEqualTo(Locale.ENGLISH);
    }

    @Test
    void an_account_without_an_address_gets_nothing() {
        adapter().deliver(new NotificationRecipient(janeId, "jane", null, Language.FR, true), greeting, null);
        adapter().deliver(new NotificationRecipient(janeId, "jane", "  ", Language.FR, true), greeting, null);

        verify(sendMail, never()).send(any());
    }
}
