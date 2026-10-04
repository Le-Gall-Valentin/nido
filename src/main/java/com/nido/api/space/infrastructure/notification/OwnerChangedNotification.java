package com.nido.api.space.infrastructure.notification;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;

/**
 * mail/space/owner-changed.html — told to the other members when a space changes owner.
 *
 * @param formerOwnerName who owned it before (see {@link OwnershipReceivedNotification})
 * @param leftNido        the former owner's account was deleted and the ownership passed on by succession
 */
@NotificationKind("space.owner-changed")
public record OwnerChangedNotification(String username, String formerOwnerName, String newOwnerName, String spaceName,
                                       boolean leftNido, AppPath membersPath)
    implements Notification, MailContent {

    @Override
    public String template() {
        return "space/owner-changed";
    }
}
