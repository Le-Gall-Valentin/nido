package com.nido.api.notifications.application.port.in;

import com.nido.api.notifications.domain.model.NotificationRequest;
import com.nido.api.notifications.domain.model.NotificationType;

/**
 * Delivers one notification now, in a transaction of its own. Called by this context once the change the
 * notification reports is committed (see {@code NotifyUseCase}); other contexts call {@code NotifyUseCase}.
 */
public interface DeliverNotificationUseCase {
    void deliver(NotificationType type, NotificationRequest request);
}
