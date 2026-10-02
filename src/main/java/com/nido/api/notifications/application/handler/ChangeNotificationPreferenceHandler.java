package com.nido.api.notifications.application.handler;

import com.nido.api.notifications.application.port.in.ChangeNotificationPreferenceUseCase;
import com.nido.api.notifications.domain.model.NotificationChannel;
import com.nido.api.notifications.domain.model.NotificationException;
import com.nido.api.notifications.domain.model.NotificationType;
import com.nido.api.notifications.domain.port.out.NotificationCatalogPort;
import com.nido.api.notifications.domain.port.out.NotificationChannelPort;
import com.nido.api.notifications.domain.port.out.NotificationPreferencesRepository;
import com.nido.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * One switch of the card. A channel the installation lacks is refused like an unknown one: the card never
 * shows it, so only a hand-written request can name it, and storing its choice would mean nothing.
 */
@ApplicationService
public class ChangeNotificationPreferenceHandler implements ChangeNotificationPreferenceUseCase {

    private final NotificationCatalogPort catalog;
    private final NotificationPreferencesRepository preferences;
    private final List<NotificationChannelPort> channels;
    private final Clock clock;

    public ChangeNotificationPreferenceHandler(NotificationCatalogPort catalog, NotificationPreferencesRepository preferences,
                                               List<NotificationChannelPort> channels, Clock clock) {
        this.catalog = catalog;
        this.preferences = preferences;
        this.channels = channels;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void changeChannel(UUID userId, String channelCode, boolean enabled) {
        NotificationChannel channel = NotificationChannel.fromCode(channelCode)
            .filter(this::isAvailable)
            .orElseThrow(NotificationException.UnknownChannel::new);
        preferences.saveChannel(userId, channel, enabled, clock.instant());
    }

    @Override
    @Transactional
    public void changeType(UUID userId, String typeCode, boolean enabled) {
        NotificationType type = catalog.catalog().find(typeCode)
            .orElseThrow(NotificationException.UnknownType::new);
        preferences.saveType(userId, type, enabled, clock.instant());
    }

    private boolean isAvailable(NotificationChannel channel) {
        return channels.stream().anyMatch(port -> port.channel() == channel && port.isAvailable());
    }
}
