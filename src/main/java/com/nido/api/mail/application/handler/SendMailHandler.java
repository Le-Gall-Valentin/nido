package com.nido.api.mail.application.handler;

import com.nido.api.mail.application.port.in.SendMailUseCase;
import com.nido.api.mail.domain.model.ActiveMail;
import com.nido.api.mail.domain.model.MailRequest;
import com.nido.api.mail.domain.model.OutgoingMail;
import com.nido.api.mail.domain.model.RenderedMail;
import com.nido.api.mail.domain.port.out.MailConfigurationPort;
import com.nido.api.mail.domain.port.out.MailDispatchTriggerPort;
import com.nido.api.mail.domain.port.out.MailOutboxPort;
import com.nido.api.mail.domain.port.out.MailRendererPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

/**
 * Sends a mail when mail is on at the moment it is asked: writes it now, queues it in the caller's transaction, and asks for a
 * dispatch once that transaction commits.
 *
 * <p>Written now rather than at delivery, for two reasons. A broken template fails the caller, where
 * it shows, instead of disappearing in the background. And a mail describes things as they were when
 * it was sent, not as they are whenever the SMTP server comes back.
 */
public class SendMailHandler implements SendMailUseCase {

    private static final Logger log = LoggerFactory.getLogger(SendMailHandler.class);

    private final MailConfigurationPort configuration;
    private final MailRendererPort renderer;
    private final MailOutboxPort outbox;
    private final MailDispatchTriggerPort trigger;
    private final Clock clock;

    public SendMailHandler(MailConfigurationPort configuration, MailRendererPort renderer, MailOutboxPort outbox,
                           MailDispatchTriggerPort trigger, Clock clock) {
        this.configuration = configuration;
        this.renderer = renderer;
        this.outbox = outbox;
        this.trigger = trigger;
        this.clock = clock;
    }

    /**
     * Mail off: accepted, and dropped — callers never ask first; a password change that would have
     * sent an alert simply sends none.
     */
    @Override
    @Transactional
    public void send(MailRequest request) {
        Optional<ActiveMail> active = configuration.active();
        if (active.isEmpty()) {
            log.debug("Mail is off: {} not sent", request.content().template());
            return;
        }
        RenderedMail rendered = renderer.render(request.content(), request.locale(), active.get().appUrl());
        outbox.enqueue(request.content().template(), new OutgoingMail(request.to(), rendered),
            clock.instant(), request.expiresAt());
        trigger.wakeUpAfterCommit();
    }
}
