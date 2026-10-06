package com.nido.api.notifications.application.port.in;

import com.nido.api.shared.model.Role;

import java.util.UUID;

public interface ChangeNotificationPreferenceUseCase {

    /** @throws com.nido.api.notifications.domain.model.NotificationException.UnknownChannel for a code that names no channel of this installation */
    void changeChannel(UUID userId, String channelCode, boolean enabled);

    /**
     * @throws com.nido.api.notifications.domain.model.NotificationException.UnknownType for a code the catalogue
     *         does not know, or a kind reserved to other roles — the card never shows it to this account
     */
    void changeType(UUID userId, Role role, String typeCode, boolean enabled);
}
