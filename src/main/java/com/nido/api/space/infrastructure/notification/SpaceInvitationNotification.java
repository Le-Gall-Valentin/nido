package com.nido.api.space.infrastructure.notification;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;

/**
 * mail/space/invitation-received.html — told to the invitee when an invitation is issued. The names are
 * the ones of that moment: the mail describes the invitation as it was sent.
 *
 * @param username        the invitee's, to greet them
 * @param invitationsPath where invitations are accepted
 */
@NotificationKind("space.invitation")
public record SpaceInvitationNotification(String username, String inviterName, String spaceName,
                                          long validityDays, AppPath invitationsPath)
    implements Notification, MailContent {

    @Override
    public String template() {
        return "space/invitation-received";
    }
}
