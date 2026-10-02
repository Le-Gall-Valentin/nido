package com.nido.api.notifications.application.port.in;

import com.nido.api.notifications.domain.model.NotificationRequest;

/**
 * The one door to tell an account something. Delivers on every channel the installation has, that can
 * write the notification, and that the account keeps on for this kind — inside the caller's transaction,
 * so a rolled-back caller tells nobody. An account that does not exist, is deactivated or has switched
 * everything off is told nothing, silently: callers never ask first.
 */
public interface NotifyUseCase {
    void notify(NotificationRequest request);
}
