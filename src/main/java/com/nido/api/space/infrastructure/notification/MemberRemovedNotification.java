package com.nido.api.space.infrastructure.notification;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;

/** mail/space/member-removed.html — told to the other members when someone is removed; not to its author. */
@NotificationKind("space.member-removed")
public record MemberRemovedNotification(String username, String actorName, String memberName, String spaceName,
                                        AppPath membersPath)
    implements Notification, MailContent {

    @Override
    public String template() {
        return "space/member-removed";
    }
}
