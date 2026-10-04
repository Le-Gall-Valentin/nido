package com.nido.api.space.infrastructure.notification;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;

/**
 * mail/space/ownership-received.html — told to the new owner of a space.
 *
 * @param formerOwnerName who owned it before: who handed it over, or whose account was deleted ({@code leftNido})
 * @param leftNido        the former owner's account was deleted and the ownership passed on by succession
 */
@NotificationKind("space.ownership-received")
public record OwnershipReceivedNotification(String username, String formerOwnerName, String spaceName, boolean leftNido,
                                            AppPath membersPath)
    implements Notification, MailContent {

    @Override
    public String template() {
        return "space/ownership-received";
    }
}
