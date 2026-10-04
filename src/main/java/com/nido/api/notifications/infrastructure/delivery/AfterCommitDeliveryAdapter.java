package com.nido.api.notifications.infrastructure.delivery;

import com.nido.api.notifications.application.port.in.DeliverNotificationUseCase;
import com.nido.api.notifications.domain.model.NotificationRequest;
import com.nido.api.notifications.domain.model.NotificationType;
import com.nido.api.notifications.domain.port.out.NotificationDeliveryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Delivers a notification right after the transaction that asked for it commits, on the same thread — as
 * AfterCommitDispatchTrigger does for mail. The delivery opens its own transaction: by then the caller's is
 * committed, and joining it would write nothing.
 *
 * <p>A delivery that fails is logged here and stops here: the change is committed already, and an exception
 * thrown after the commit would only turn its success into an error. Each notification is delivered on its
 * own, so one that fails does not stop the next.
 */
@Component
public class AfterCommitDeliveryAdapter implements NotificationDeliveryPort {

    private static final Logger log = LoggerFactory.getLogger(AfterCommitDeliveryAdapter.class);

    private final DeliverNotificationUseCase deliver;

    public AfterCommitDeliveryAdapter(DeliverNotificationUseCase deliver) {
        this.deliver = deliver;
    }

    @Override
    public void deliverAfterCommit(NotificationType type, NotificationRequest request) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deliverNow(type, request);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deliverNow(type, request);
            }
        });
    }

    private void deliverNow(NotificationType type, NotificationRequest request) {
        try {
            deliver.deliver(type, request);
        } catch (RuntimeException e) {
            log.error("Notification {} to account {} not delivered; the change it reports stands",
                type.code(), request.recipientId(), e);
        }
    }
}
