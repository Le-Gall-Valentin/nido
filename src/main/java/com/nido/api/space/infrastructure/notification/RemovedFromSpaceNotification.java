package com.nido.api.space.infrastructure.notification;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;

/** mail/space/removed.html — told to the person removed from a space, who no longer has access to it. */
@NotificationKind("space.removed")
public record RemovedFromSpaceNotification(String username, String actorName, String spaceName, AppPath spacesPath)
    implements Notification, MailContent {

    @Override
    public String template() {
        return "space/removed";
    }
}
