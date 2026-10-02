package com.nido.api.space.infrastructure.notification;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;

/** mail/space/member-joined.html — told to the other members when someone joins a space. */
@NotificationKind("space.member-joined")
public record MemberJoinedNotification(String username, String memberName, String spaceName, AppPath membersPath)
    implements Notification, MailContent {

    @Override
    public String template() {
        return "space/member-joined";
    }
}
