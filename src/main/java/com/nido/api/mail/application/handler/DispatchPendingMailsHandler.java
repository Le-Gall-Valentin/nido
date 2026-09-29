package com.nido.api.mail.application.handler;

import com.nido.api.mail.application.port.in.DispatchPendingMailsUseCase;
import com.nido.api.mail.domain.model.DeliveryOutcome;
import com.nido.api.mail.domain.model.OutboxEntry;
import com.nido.api.mail.domain.model.RetryPolicy;
import com.nido.api.mail.domain.port.out.MailOutboxPort;
import com.nido.api.mail.domain.port.out.MailTransportPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Sends what is due, batch after batch until a batch comes back short.
 *
 * <p>Not transactional: each claim commits on its own, so the lease is visible to every other
 * dispatcher before the SMTP exchange starts, and no transaction stays open while a server takes its
 * time. A dispatcher that dies mid-send leaves its mails leased; the lease runs out and the next
 * claim picks them up.
 */
public class DispatchPendingMailsHandler implements DispatchPendingMailsUseCase {

    private static final Logger log = LoggerFactory.getLogger(DispatchPendingMailsHandler.class);

    static final int BATCH_SIZE = 20;
    static final Duration LEASE = Duration.ofMinutes(2);

    private final MailOutboxPort outbox;
    private final MailTransportPort transport;
    private final RetryPolicy retryPolicy;
    private final Clock clock;

    public DispatchPendingMailsHandler(MailOutboxPort outbox, MailTransportPort transport, RetryPolicy retryPolicy, Clock clock) {
        this.outbox = outbox;
        this.transport = transport;
        this.retryPolicy = retryPolicy;
        this.clock = clock;
    }

    @Override
    public int dispatch() {
        int handled = 0;
        List<OutboxEntry> batch;
        do {
            Instant now = clock.instant();
            batch = outbox.claimDue(now, BATCH_SIZE, LEASE);
            batch.forEach(entry -> handle(entry, now));
            handled += batch.size();
        } while (batch.size() == BATCH_SIZE);
        return handled;
    }

    private void handle(OutboxEntry entry, Instant now) {
        if (entry.isExpiredAt(now)) {
            outbox.delete(entry.id());
            log.warn("Mail {} ({}) expired before it could be delivered and is dropped", entry.id(), entry.kind());
            return;
        }
        DeliveryOutcome outcome;
        try {
            outcome = transport.deliver(entry.mail());
        } catch (RuntimeException e) {
            outcome = new DeliveryOutcome.TemporaryFailure(e.getClass().getSimpleName());
        }
        switch (outcome) {
            case DeliveryOutcome.Sent sent -> outbox.delete(entry.id());
            case DeliveryOutcome.PermanentFailure failure -> {
                outbox.delete(entry.id());
                log.error("Mail {} ({}) was refused and is dropped: {}", entry.id(), entry.kind(), failure.reason());
            }
            case DeliveryOutcome.TemporaryFailure failure -> retryLater(entry, failure, now);
        }
    }

    private void retryLater(OutboxEntry entry, DeliveryOutcome.TemporaryFailure failure, Instant now) {
        int failedAttempts = entry.attempts() + 1;
        retryPolicy.nextAttempt(failedAttempts, now, entry.expiresAt()).ifPresentOrElse(
            next -> {
                outbox.reschedule(entry.id(), failedAttempts, next, failure.reason());
                log.error("Mail {} ({}) could not be delivered (attempt {}), next try at {}: {}",
                    entry.id(), entry.kind(), failedAttempts, next, failure.reason());
            },
            () -> {
                outbox.delete(entry.id());
                log.error("Mail {} ({}) could not be delivered after {} attempts and is dropped: {}",
                    entry.id(), entry.kind(), failedAttempts, failure.reason());
            });
    }
}
