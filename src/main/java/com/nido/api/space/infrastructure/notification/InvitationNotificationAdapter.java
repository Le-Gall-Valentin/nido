package com.nido.api.space.infrastructure.notification;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.notifications.application.port.in.NotifyUseCase;
import com.nido.api.notifications.domain.model.NotificationRequest;
import com.nido.api.space.domain.model.InviteMemberCommand;
import com.nido.api.space.domain.model.SpaceInvitation;
import com.nido.api.space.domain.port.out.InvitationNotificationPort;
import org.springframework.stereotype.Component;

/** The space context's notifications, sent through the notifications context. */
@Component
public class InvitationNotificationAdapter implements InvitationNotificationPort {

    /** Where an invitation is accepted: the spaces page lists the ones waiting for an answer. */
    private static final AppPath INVITATIONS = new AppPath("/spaces");

    private final NotifyUseCase notify;

    public InvitationNotificationAdapter(NotifyUseCase notify) {
        this.notify = notify;
    }

    @Override
    public void invitationIssued(SpaceInvitation invitation, String inviteeName, String inviterName, String spaceName) {
        notify.notify(new NotificationRequest(invitation.inviteeId(),
            new SpaceInvitationNotification(inviteeName, inviterName, spaceName,
                InviteMemberCommand.VALIDITY.toDays(), INVITATIONS),
            invitation.expiresAt()));
    }
}
