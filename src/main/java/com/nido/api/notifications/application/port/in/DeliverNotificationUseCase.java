package com.nido.api.notifications.application.port.in;

import com.nido.api.notifications.domain.model.NotificationRequest;
import com.nido.api.notifications.domain.model.NotificationType;
import com.nido.api.shared.model.Language;

/**
 * Delivers one notification now, in a transaction of its own. Called by this context once the change the
 * notification reports is committed (see {@code NotifyUseCase}); other contexts call {@code NotifyUseCase}.
 */
public interface DeliverNotificationUseCase {

    /**
     * @param actorLanguage the language of whoever's gesture this notification reports, when known: an account
     *                      without a language of its own is written to in it (an invitee, in the inviter's)
     */
    void deliver(NotificationType type, NotificationRequest request, Language actorLanguage);
}
