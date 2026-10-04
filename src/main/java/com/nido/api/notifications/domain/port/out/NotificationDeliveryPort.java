package com.nido.api.notifications.domain.port.out;

import com.nido.api.notifications.domain.model.NotificationRequest;
import com.nido.api.notifications.domain.model.NotificationType;

/** When a notification leaves: once the transaction that asked for it commits, never inside it. */
public interface NotificationDeliveryPort {

    /**
     * Delivers after the current transaction commits — right away when there is none, never when it rolls
     * back. A delivery that fails is logged and goes no further: the change it reports stands.
     */
    void deliverAfterCommit(NotificationType type, NotificationRequest request);
}
