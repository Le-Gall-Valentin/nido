package com.nido.api.mail.application.handler;

import com.nido.api.mail.KitSampleMail;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailRequest;
import com.nido.api.mail.domain.model.OutgoingMail;
import com.nido.api.mail.domain.model.Recipient;
import com.nido.api.mail.domain.model.RenderedMail;
import com.nido.api.mail.domain.port.out.MailDispatchTriggerPort;
import com.nido.api.mail.domain.port.out.MailOutboxPort;
import com.nido.api.mail.domain.port.out.MailRendererPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendMailHandlerTest {

    @Mock MailRendererPort renderer;
    @Mock MailOutboxPort outbox;
    @Mock MailDispatchTriggerPort trigger;

    private final Instant now = Instant.parse("2026-09-28T10:00:00Z");
    private final Recipient jane = new Recipient("jane@example.com", "Jane");
    private final KitSampleMail content = new KitSampleMail("jane", new AppPath("/x"));

    private SendMailHandler handler() {
        return new SendMailHandler(renderer, outbox, trigger, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void writes_the_mail_queues_it_then_asks_for_a_dispatch_after_commit() {
        RenderedMail rendered = new RenderedMail("Subject", "<p>html</p>", "text");
        when(renderer.render(content, Locale.FRENCH)).thenReturn(rendered);
        Instant expiresAt = now.plusSeconds(1800);

        handler().send(new MailRequest(jane, Locale.FRENCH, content, expiresAt));

        InOrder order = inOrder(renderer, outbox, trigger);
        order.verify(renderer).render(content, Locale.FRENCH);
        order.verify(outbox).enqueue("test/kit-sample", new OutgoingMail(jane, rendered), now, expiresAt);
        order.verify(trigger).wakeUpAfterCommit();
    }

    @Test
    void a_template_that_cannot_be_written_fails_the_caller_and_queues_nothing() {
        when(renderer.render(any(), any())).thenThrow(new IllegalStateException("Mail message 'x' is missing"));

        assertThatThrownBy(() -> handler().send(MailRequest.of(jane, Locale.FRENCH, content)))
            .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(outbox, trigger);
    }

    @Test
    void with_mail_off_a_mail_is_accepted_and_goes_nowhere() {
        new DisabledSendMailHandler().send(MailRequest.of(jane, Locale.FRENCH, content));

        verifyNoInteractions(renderer, outbox, trigger);
    }
}
