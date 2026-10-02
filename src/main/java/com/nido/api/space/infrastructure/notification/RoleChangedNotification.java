package com.nido.api.space.infrastructure.notification;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;
import com.nido.api.space.domain.model.SpaceRole;

/** mail/space/role-changed.html — told to the member whose role an admin or the owner changed. */
@NotificationKind("space.role-changed")
public record RoleChangedNotification(String username, String actorName, String spaceName,
                                      SpaceRole previousRole, SpaceRole newRole, AppPath membersPath)
    implements Notification, MailContent {

    @Override
    public String template() {
        return "space/role-changed";
    }
}
