package com.nido.api.space.infrastructure.notification;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;

/** mail/space/deleted.html — told to the members of a space its owner deleted. */
@NotificationKind("space.deleted")
public record SpaceDeletedNotification(String username, String actorName, String spaceName, AppPath spacesPath)
    implements Notification, MailContent {

    @Override
    public String template() {
        return "space/deleted";
    }
}
