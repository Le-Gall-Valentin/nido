package com.nido.api.notifications.application.handler;

import com.nido.api.notifications.application.port.in.NotifyUseCase;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationRequest;
import com.nido.api.notifications.domain.model.NotificationType;
import com.nido.api.notifications.domain.port.out.NotificationCatalogPort;
import com.nido.api.notifications.domain.port.out.NotificationChannelPort;
import com.nido.api.notifications.domain.port.out.NotificationDeliveryPort;
import com.nido.api.shared.annotation.ApplicationService;

import java.util.List;

/**
 * Tells an account something once the change it reports is committed. The kind is read now, so an
 * uncatalogued notification fails where it is sent; the delivery waits for the caller's commit and runs on
 * its own — a caller that rolls back tells nobody, and a notification that fails never undoes the change.
 */
@ApplicationService
public class NotifyHandler implements NotifyUseCase {

    private final NotificationCatalogPort catalog;
    private final List<NotificationChannelPort> channels;
    private final NotificationDeliveryPort delivery;

    public NotifyHandler(NotificationCatalogPort catalog, List<NotificationChannelPort> channels,
                         NotificationDeliveryPort delivery) {
        this.catalog = catalog;
        this.channels = channels;
        this.delivery = delivery;
    }

    @Override
    public void notify(NotificationRequest request) {
        Class<? extends Notification> notificationClass = request.notification().getClass();
        NotificationType type = catalog.catalog().typeOf(notificationClass);
        // No channel for it on this installation (mail off): nothing to wait for, no transaction to open.
        if (channels.stream().anyMatch(channel -> channel.canDeliver(notificationClass))) {
            delivery.deliverAfterCommit(type, request);
        }
    }
}
