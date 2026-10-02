package com.nido.api.space.infrastructure.notification;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;

/**
 * mail/space/owner-changed.html — told to the other members when a space changes owner.
 *
 * @param actorName the former owner who handed it over; {@code null} after a succession (see
 *                  {@link OwnershipReceivedNotification})
 */
@NotificationKind("space.owner-changed")
public record OwnerChangedNotification(String username, String actorName, String newOwnerName, String spaceName,
                                       AppPath membersPath)
    implements Notification, MailContent {

    @Override
    public String template() {
        return "space/owner-changed";
    }
}
