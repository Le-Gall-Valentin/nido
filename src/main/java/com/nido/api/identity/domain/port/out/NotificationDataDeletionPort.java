package com.nido.api.identity.domain.port.out;

import java.util.UUID;

public interface NotificationDataDeletionPort {
    /** Forgets what the account chose to receive. The account row is anonymised, not deleted, so nothing cascades. */
    void deleteNotificationData(UUID userId);
}
