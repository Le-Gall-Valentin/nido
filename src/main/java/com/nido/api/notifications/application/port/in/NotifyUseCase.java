package com.nido.api.notifications.application.port.in;

import com.nido.api.notifications.domain.model.NotificationRequest;

/**
 * The one door to tell an account something. Delivered once the caller's transaction commits — a
 * rolled-back caller tells nobody, and a notification that cannot be delivered is logged, never undoing the
 * caller's change — on every channel the installation has, that can write it, and that the account keeps on
 * for this kind. An account that does not exist, is deactivated or has switched everything off is told
 * nothing, silently: callers never ask first.
 */
public interface NotifyUseCase {
    void notify(NotificationRequest request);
}
