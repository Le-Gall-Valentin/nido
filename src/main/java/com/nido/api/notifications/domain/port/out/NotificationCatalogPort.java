package com.nido.api.notifications.domain.port.out;

import com.nido.api.notifications.domain.model.NotificationCatalog;

/** The kinds of notification the application declares — how they were found is the adapter's business. */
public interface NotificationCatalogPort {
    NotificationCatalog catalog();
}
