package com.nido.api.notifications.domain.port.out;

import com.nido.api.notifications.domain.model.NotificationRecipient;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRecipientPort {
    /** Empty for an account that does not exist or was deleted; a deactivated one is found, and says so. */
    Optional<NotificationRecipient> find(UUID userId);
}
