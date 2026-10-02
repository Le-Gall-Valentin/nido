package com.nido.api.space.infrastructure.notification;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;

/**
 * mail/space/ownership-received.html — told to the new owner of a space.
 *
 * @param actorName the former owner who handed it over; {@code null} when the ownership passed on because
 *                  the former owner's account was deleted — whose name is erased by then, and must not be told
 */
@NotificationKind("space.ownership-received")
public record OwnershipReceivedNotification(String username, String actorName, String spaceName, AppPath membersPath)
    implements Notification, MailContent {

    @Override
    public String template() {
        return "space/ownership-received";
    }
}
