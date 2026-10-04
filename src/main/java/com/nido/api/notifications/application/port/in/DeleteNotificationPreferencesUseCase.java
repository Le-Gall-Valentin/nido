package com.nido.api.notifications.application.port.in;

import java.util.UUID;

/** Forgets an account's choices — for the GDPR deletion, which keeps the account row and so never cascades. */
public interface DeleteNotificationPreferencesUseCase {
    void deleteAllFor(UUID userId);
}
