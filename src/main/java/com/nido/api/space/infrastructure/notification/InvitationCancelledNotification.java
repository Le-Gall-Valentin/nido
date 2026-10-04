package com.nido.api.space.infrastructure.notification;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;
import com.nido.api.space.domain.model.InvitationCancellation;

/**
 * mail/space/invitation-cancelled.html — told to the invitee of a pending invitation that stopped being valid.
 *
 * @param actorName who revoked it, deleted the space, or — {@code OWNER_LEFT_NIDO} — whose account was deleted
 */
@NotificationKind("space.invitation-cancelled")
public record InvitationCancelledNotification(String username, String actorName, String spaceName,
                                              InvitationCancellation reason, AppPath spacesPath)
    implements Notification, MailContent {

    @Override
    public String template() {
        return "space/invitation-cancelled";
    }
}
