package com.nido.api.notifications.application.handler;

import com.nido.api.notifications.application.port.in.GetNotificationPreferencesUseCase;
import com.nido.api.notifications.domain.model.NotificationPreferences;
import com.nido.api.notifications.domain.model.NotificationPreferencesView;
import com.nido.api.notifications.domain.model.NotificationPreferencesView.ChannelSetting;
import com.nido.api.notifications.domain.model.NotificationPreferencesView.TypeSetting;
import com.nido.api.notifications.domain.port.out.NotificationCatalogPort;
import com.nido.api.notifications.domain.port.out.NotificationChannelPort;
import com.nido.api.notifications.domain.port.out.NotificationPreferencesRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.shared.model.Role;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * The preferences card. Only the channels this installation has: a mail switch on an installation
 * without SMTP would promise mails that never come. Every kind the catalogue opens to the account's role, whatever the channels:
 * their state is kept for when a channel appears. A choice stored for a kind the code no longer declares
 * is simply never asked about.
 */
@ApplicationService
public class GetNotificationPreferencesHandler implements GetNotificationPreferencesUseCase {

    private final NotificationCatalogPort catalog;
    private final NotificationPreferencesRepository preferences;
    private final List<NotificationChannelPort> channels;

    public GetNotificationPreferencesHandler(NotificationCatalogPort catalog, NotificationPreferencesRepository preferences,
                                             List<NotificationChannelPort> channels) {
        this.catalog = catalog;
        this.preferences = preferences;
        this.channels = channels;
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationPreferencesView get(UUID userId, Role role) {
        NotificationPreferences chosen = preferences.find(userId);
        List<ChannelSetting> available = channels.stream()
            .filter(NotificationChannelPort::isAvailable)
            .map(NotificationChannelPort::channel)
            .distinct()
            .sorted()
            .map(channel -> new ChannelSetting(channel, chosen.isOn(channel)))
            .toList();
        List<TypeSetting> types = catalog.catalog().typesFor(role).stream()
            .map(type -> new TypeSetting(type, chosen.isOn(type)))
            .toList();
        return new NotificationPreferencesView(available, types);
    }
}
