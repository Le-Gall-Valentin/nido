package com.nido.api.notifications.application.handler;

import com.nido.api.notifications.application.port.in.NotifyUseCase;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationPreferences;
import com.nido.api.notifications.domain.model.NotificationRecipient;
import com.nido.api.notifications.domain.model.NotificationRequest;
import com.nido.api.notifications.domain.model.NotificationType;
import com.nido.api.notifications.domain.port.out.NotificationCatalogPort;
import com.nido.api.notifications.domain.port.out.NotificationChannelPort;
import com.nido.api.notifications.domain.port.out.NotificationPreferencesRepository;
import com.nido.api.notifications.domain.port.out.NotificationRecipientPort;
import com.nido.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Tells an account, on every channel that can: the installation has it, it can write this notification,
 * and the account keeps both the channel and the kind on.
 *
 * <p>Runs in the caller's transaction: a channel queues its delivery there (mail: the outbox), so a
 * caller that rolls back tells nobody. A channel that fails — a broken template — fails the caller too,
 * which is where a bug should show; catching it would not help anyway, since the transaction is already
 * marked for rollback by then.
 */
@ApplicationService
public class NotifyHandler implements NotifyUseCase {

    private static final Logger log = LoggerFactory.getLogger(NotifyHandler.class);

    private final NotificationCatalogPort catalog;
    private final NotificationRecipientPort recipients;
    private final NotificationPreferencesRepository preferences;
    private final List<NotificationChannelPort> channels;

    public NotifyHandler(NotificationCatalogPort catalog, NotificationRecipientPort recipients,
                         NotificationPreferencesRepository preferences, List<NotificationChannelPort> channels) {
        this.catalog = catalog;
        this.recipients = recipients;
        this.preferences = preferences;
        this.channels = channels;
    }

    @Override
    @Transactional
    public void notify(NotificationRequest request) {
        Notification notification = request.notification();
        NotificationType type = catalog.catalog().typeOf(notification.getClass());
        List<NotificationChannelPort> able = channels.stream()
            .filter(channel -> channel.isAvailable() && channel.supports(notification.getClass()))
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
