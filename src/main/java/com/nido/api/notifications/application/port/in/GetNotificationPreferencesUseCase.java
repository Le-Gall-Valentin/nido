package com.nido.api.notifications.application.port.in;

import com.nido.api.notifications.domain.model.NotificationPreferencesView;

import com.nido.api.shared.model.Role;

import java.util.UUID;

public interface GetNotificationPreferencesUseCase {
    /** The channels this installation has and every kind open to the account's role, each with the account's choice. */
    NotificationPreferencesView get(UUID userId, Role role);
}
