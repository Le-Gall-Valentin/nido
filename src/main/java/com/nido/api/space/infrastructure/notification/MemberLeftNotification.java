package com.nido.api.space.infrastructure.notification;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;

/**
 * mail/space/member-left.html — told to the remaining members when someone leaves a space.
 *
 * @param leftNido the member's account was deleted: they left Nido, and so the space
 */
@NotificationKind("space.member-left")
public record MemberLeftNotification(String username, String memberName, String spaceName, boolean leftNido,
                                     AppPath membersPath)
    implements Notification, MailContent {

    @Override
    public String template() {
        return "space/member-left";
    }
}
