package com.nido.api.notifications.application.handler;

import com.nido.api.notifications.application.port.in.DeliverNotificationUseCase;
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
import com.nido.api.shared.model.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Delivers a notification on every channel that can: the installation has it, it can write this
 * notification, and the account — existing, active, and of a role the kind is open to — keeps both the channel and the kind on.
 *
 * An account without a language of its own is written to in the language of whoever acted. A request may name
 * a fallback: on a channel where the account switched the main kind off, the fallback goes instead, if the
 * account keeps its kind on.
 *
 * <p>Alone in its transaction: it runs once the change it reports is committed (see NotifyHandler), so
 * nothing here can undo that change, and a failure here rolls back only what this delivery wrote.
 */
@ApplicationService
public class DeliverNotificationHandler implements DeliverNotificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeliverNotificationHandler.class);

    private final NotificationCatalogPort catalog;
    private final NotificationRecipientPort recipients;
    private final NotificationPreferencesRepository preferences;
    private final List<NotificationChannelPort> channels;

    public DeliverNotificationHandler(NotificationCatalogPort catalog, NotificationRecipientPort recipients,
                                      NotificationPreferencesRepository preferences, List<NotificationChannelPort> channels) {
        this.catalog = catalog;
        this.recipients = recipients;
        this.preferences = preferences;
        this.channels = channels;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliver(NotificationType type, NotificationRequest request, Language actorLanguage) {
        List<Candidate> candidates = candidates(type, request);
        List<NotificationChannelPort> able = channels.stream()
            .filter(channel -> candidates.stream().anyMatch(candidate -> candidate.travelsOn(channel)))
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
        List<Candidate> open = candidates.stream()
            .filter(candidate -> catalog.catalog().isOpenTo(candidate.type(), recipient.get().role()))
            .toList();
        if (open.isEmpty()) {
            log.debug("Account {} no longer has a role {} is reserved to: not sent", request.recipientId(), type.code());
            return;
        }
        NotificationPreferences chosen = preferences.find(request.recipientId());
        NotificationRecipient addressed = recipient.get().withLanguageOr(actorLanguage);
        for (NotificationChannelPort channel : able) {
            // The main notification when the account keeps its kind on this channel, else the fallback when it keeps that one's.
            open.stream()
                .filter(candidate -> candidate.travelsOn(channel) && chosen.allows(candidate.type(), channel.channel()))
                .findFirst()
                .ifPresent(candidate -> channel.deliver(addressed, candidate.type(), candidate.notification(), request.expiresAt()));
        }
    }

    /** The main notification, then its fallback when the request names one. */
    private List<Candidate> candidates(NotificationType type, NotificationRequest request) {
        Notification fallback = request.fallback();
        return fallback == null
            ? List.of(new Candidate(type, request.notification()))
            : List.of(new Candidate(type, request.notification()),
                      new Candidate(catalog.catalog().typeOf(fallback.getClass()), fallback));
    }

    private record Candidate(NotificationType type, Notification notification) {

        boolean travelsOn(NotificationChannelPort channel) {
            return channel.canDeliver(notification.getClass());
        }
    }
}
