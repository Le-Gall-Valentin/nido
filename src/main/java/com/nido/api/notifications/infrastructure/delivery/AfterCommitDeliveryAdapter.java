package com.nido.api.notifications.infrastructure.delivery;

import com.nido.api.infrastructure.web.MailLanguage;
import com.nido.api.notifications.application.port.in.DeliverNotificationUseCase;
import com.nido.api.notifications.domain.model.NotificationRequest;
import com.nido.api.notifications.domain.model.NotificationType;
import com.nido.api.notifications.domain.port.out.NotificationDeliveryPort;
import com.nido.api.shared.model.Language;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Delivers a notification once the transaction that asked for it commits, on a worker thread of its own — as
 * AfterCommitDispatchTrigger does for mail. Not on the committing thread: there, the caller's connection is still
 * held until its transaction is cleaned up, and the delivery's own transaction would hold a second one — under a
 * burst of requests, enough to empty the pool. One worker: deliveries never compete with each other for it.
 *
 * <p>What the delivery would have read from the request is read before it leaves: the language of whoever acts,
 * for an account without its own. A delivery that fails is logged here and stops here — by its class only, since
 * an exception's message can quote what it was writing; the change it reports is committed already, and each
 * notification is delivered on its own, so one that fails does not stop the next.
 */
@Component
public class AfterCommitDeliveryAdapter implements NotificationDeliveryPort {

    private static final Logger log = LoggerFactory.getLogger(AfterCommitDeliveryAdapter.class);

    private final DeliverNotificationUseCase deliver;
    private final Executor worker;

    @Autowired
    public AfterCommitDeliveryAdapter(DeliverNotificationUseCase deliver) {
        this(deliver, Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "notification-delivery");
            thread.setDaemon(true);
            return thread;
        }));
    }

    AfterCommitDeliveryAdapter(DeliverNotificationUseCase deliver, Executor worker) {
        this.deliver = deliver;
        this.worker = worker;
    }

    @Override
    public void deliverAfterCommit(NotificationType type, NotificationRequest request) {
        // Read now, on the request's thread: the worker has no request to ask, and this one may be gone by then.
        Language actorLanguage = MailLanguage.requested().orElse(null);
        Runnable delivery = () -> deliverNow(type, request, actorLanguage);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            handOver(type, request, delivery);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                handOver(type, request, delivery);
            }
        });
    }

    private void handOver(NotificationType type, NotificationRequest request, Runnable delivery) {
        try {
            worker.execute(delivery);
        } catch (RejectedExecutionException shuttingDown) {
            log.warn("Notification {} to account {} not delivered: the application is shutting down",
                type.code(), request.recipientId());
        }
    }

    private void deliverNow(NotificationType type, NotificationRequest request, Language actorLanguage) {
        try {
            deliver.deliver(type, request, actorLanguage);
        } catch (RuntimeException e) {
            log.error("Notification {} to account {} not delivered ({}); the change it reports stands",
                type.code(), request.recipientId(), e.getClass().getSimpleName());
            log.debug("Why notification {} to account {} was not delivered", type.code(), request.recipientId(), e);
        }
    }

    /** Lets the deliveries already handed over finish: their changes are committed, their mails are owed. */
    @PreDestroy
    void shutdown() throws InterruptedException {
        if (worker instanceof ExecutorService service) {
            service.shutdown();
            if (!service.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("Notification deliveries still pending at shutdown were dropped");
                service.shutdownNow();
            }
        }
    }
}
