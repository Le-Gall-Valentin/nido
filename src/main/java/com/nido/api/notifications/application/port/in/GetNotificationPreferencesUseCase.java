package com.nido.api.notifications.application.port.in;

import com.nido.api.notifications.domain.model.NotificationPreferencesView;

import java.util.UUID;

public interface GetNotificationPreferencesUseCase {
    /** The channels this installation has and every kind the application declares, each with the account's choice. */
    NotificationPreferencesView get(UUID userId);
}
