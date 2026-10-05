package com.nido.api.mail.application.handler;

import com.nido.api.mail.domain.model.ActiveMail;
import com.nido.api.mail.domain.model.DeliveryOutcome;
import com.nido.api.mail.domain.model.OutboxEntry;
import com.nido.api.mail.domain.model.OutgoingMail;
import com.nido.api.mail.domain.model.Recipient;
import com.nido.api.mail.domain.model.RenderedMail;
import com.nido.api.mail.domain.model.RetryPolicy;
import com.nido.api.mail.domain.port.out.MailConfigurationPort;
import com.nido.api.mail.domain.port.out.MailOutboxPort;
import com.nido.api.mail.domain.port.out.MailTransportPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchPendingMailsHandlerTest {

    @Mock MailConfigurationPort configuration;
    @Mock MailOutboxPort outbox;
    @Mock MailTransportPort transport;

    private final Instant now = Instant.parse("2026-09-28T10:00:00Z");

    private DispatchPendingMailsHandler handler() {
        return new DispatchPendingMailsHandler(configuration, outbox, transport, new RetryPolicy(), Clock.fixed(now, ZoneOffset.UTC));
    }

    @BeforeEach
    void mailIsOn() {
        lenient().when(configuration.active()).thenReturn(Optional.of(new ActiveMail("https://nido.example")));
    }

    @Test
    void mail_off_claims_nothing() {
        when(configuration.active()).thenReturn(Optional.empty());

        assertThat(handler().dispatch()).isZero();
        verifyNoInteractions(outbox, transport);
    }

    private static OutboxEntry entry(int attempts, Instant expiresAt) {
        return new OutboxEntry(UUID.randomUUID(), "test/kit-sample",
            new OutgoingMail(new Recipient(UUID.randomUUID() + "@example.com", null), new RenderedMail("s", "h", "t")), attempts, expiresAt);
    }

    @Test
    void a_sent_mail_leaves_the_outbox() {
        OutboxEntry entry = entry(0, null);
        when(outbox.claimDue(now, 20, Duration.ofMinutes(2))).thenReturn(List.of(entry));
        when(transport.deliver(entry.mail())).thenReturn(new DeliveryOutcome.Sent());

        assertThat(handler().dispatch()).isEqualTo(1);

        verify(outbox).delete(entry.id());
    }

    @Test
    void a_refused_mail_leaves_the_outbox_without_a_retry() {
        OutboxEntry entry = entry(0, null);
        when(outbox.claimDue(any(), anyInt(), any())).thenReturn(List.of(entry));
        when(transport.deliver(entry.mail())).thenReturn(new DeliveryOutcome.PermanentFailure("SendFailedException"));

        handler().dispatch();

        verify(outbox).delete(entry.id());
        verify(outbox, never()).reschedule(any(), anyInt(), any(), any());
    }

    @Test
    void a_temporary_failure_is_tried_again_later() {
        OutboxEntry entry = entry(0, null);
        when(outbox.claimDue(any(), anyInt(), any())).thenReturn(List.of(entry));
        when(transport.deliver(entry.mail())).thenReturn(new DeliveryOutcome.TemporaryFailure("MailSendException"));

        handler().dispatch();

        verify(outbox).reschedule(entry.id(), 1, now.plus(Duration.ofMinutes(1)), "MailSendException");
        verify(outbox, never()).delete(any());
    }

    @Test
    void the_last_allowed_failure_drops_the_mail() {
        OutboxEntry entry = entry(RetryPolicy.MAX_ATTEMPTS - 1, null);
        when(outbox.claimDue(any(), anyInt(), any())).thenReturn(List.of(entry));
        when(transport.deliver(entry.mail())).thenReturn(new DeliveryOutcome.TemporaryFailure("MailSendException"));

        handler().dispatch();

        verify(outbox).delete(entry.id());
        verify(outbox, never()).reschedule(any(), anyInt(), any(), any());
    }

    @Test
    void an_expired_mail_is_dropped_without_being_sent() {
        OutboxEntry entry = entry(0, now);
        when(outbox.claimDue(any(), anyInt(), any())).thenReturn(List.of(entry));

        handler().dispatch();

        verify(outbox).delete(entry.id());
        verify(transport, never()).deliver(any());
    }

    @Test
    void a_transport_that_throws_is_a_temporary_failure_not_the_end_of_the_batch() {
        OutboxEntry first = entry(0, null);
        OutboxEntry second = entry(0, null);
        when(outbox.claimDue(any(), anyInt(), any())).thenReturn(List.of(first, second));
        when(transport.deliver(first.mail())).thenThrow(new IllegalStateException("boom"));
        when(transport.deliver(second.mail())).thenReturn(new DeliveryOutcome.Sent());

        handler().dispatch();

        verify(outbox).reschedule(eq(first.id()), eq(1), any(), eq("IllegalStateException"));
        verify(outbox).delete(second.id());
    }

    @Test
    void drains_every_due_batch_in_one_run() {
        List<OutboxEntry> full = IntStream.range(0, 20).mapToObj(i -> entry(0, null)).toList();
        List<OutboxEntry> rest = IntStream.range(0, 5).mapToObj(i -> entry(0, null)).toList();
        when(outbox.claimDue(any(), anyInt(), any())).thenReturn(full, full, rest);
        when(transport.deliver(any())).thenReturn(new DeliveryOutcome.Sent());

        assertThat(handler().dispatch()).isEqualTo(45);

        verify(outbox, times(3)).claimDue(any(), anyInt(), any());
    }

    @Test
    void nothing_due_is_nothing_done() {
        when(outbox.claimDue(any(), anyInt(), any())).thenReturn(List.of());

        assertThat(handler().dispatch()).isZero();
    }
}
