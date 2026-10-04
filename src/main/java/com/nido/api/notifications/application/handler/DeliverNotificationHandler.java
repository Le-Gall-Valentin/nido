package com.nido.api.notifications.application.handler;

import com.nido.api.notifications.application.port.in.DeliverNotificationUseCase;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationPreferences;
import com.nido.api.notifications.domain.model.NotificationRecipient;
import com.nido.api.notifications.domain.model.NotificationRequest;
import com.nido.api.notifications.domain.model.NotificationType;
import com.nido.api.notifications.domain.port.out.NotificationChannelPort;
import com.nido.api.notifications.domain.port.out.NotificationPreferencesRepository;
import com.nido.api.notifications.domain.port.out.NotificationRecipientPort;
import com.nido.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Delivers a notification on every channel that can: the installation has it, it can write this
 * notification, and the account — existing and active — keeps both the channel and the kind on.
 *
 * <p>Alone in its transaction: it runs once the change it reports is committed (see NotifyHandler), so
 * nothing here can undo that change, and a failure here rolls back only what this delivery wrote.
 */
@ApplicationService
public class DeliverNotificationHandler implements DeliverNotificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeliverNotificationHandler.class);

    private final NotificationRecipientPort recipients;
    private final NotificationPreferencesRepository preferences;
    private final List<NotificationChannelPort> channels;

    public DeliverNotificationHandler(NotificationRecipientPort recipients, NotificationPreferencesRepository preferences,
                                      List<NotificationChannelPort> channels) {
        this.recipients = recipients;
        this.preferences = preferences;
        this.channels = channels;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliver(NotificationType type, NotificationRequest request) {
        Notification notification = request.notification();
        List<NotificationChannelPort> able = channels.stream()
            .filter(channel -> channel.canDeliver(notification.getClass()))
            .toList();
        if (able.isEmpty()) {
            return;
        }
        Optional<NotificationRecipient> recipient = recipients.find(request.recipientId())
            .filter(NotificationRecipient::active);
        if (recipient.isEmpty()) {
            log.debug("Account {} cannot be notified (absent or deactivated): {} not sent",
                request.recipientId(), type.code());
            return;
        }
        NotificationPreferences chosen = preferences.find(request.recipientId());
        for (NotificationChannelPort channel : able) {
            if (chosen.allows(type, channel.channel())) {
                channel.deliver(recipient.get(), type, notification, request.expiresAt());
            }
        }
    }
}
