package com.nido.api.space.infrastructure.notification;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.notifications.application.port.in.NotifyUseCase;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationRequest;
import com.nido.api.space.domain.model.Addressee;
import com.nido.api.space.domain.model.InvitationCancellation;
import com.nido.api.space.domain.model.InviteMemberCommand;
import com.nido.api.space.domain.model.SpaceInvitation;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.port.out.SpaceNotificationPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * The space context's notifications, sent through the notifications context: one request per reader, so
 * each mail greets the person it goes to and is written in their language.
 */
@Component
public class SpaceNotificationAdapter implements SpaceNotificationPort {

    /** Where a reader lands who no longer belongs to the space, and where invitations are accepted. */
    private static final AppPath SPACES = new AppPath("/spaces");

    private final NotifyUseCase notify;

    public SpaceNotificationAdapter(NotifyUseCase notify) {
        this.notify = notify;
    }

    @Override
    public void invitationIssued(SpaceInvitation invitation, String inviteeName, String inviterName, String spaceName) {
        notify.notify(new NotificationRequest(invitation.inviteeId(),
            new SpaceInvitationNotification(inviteeName, inviterName, spaceName,
                InviteMemberCommand.VALIDITY.toDays(), SPACES),
            invitation.expiresAt()));
    }

    @Override
    public void invitationCancelled(String spaceName, String actorName, InvitationCancellation reason,
                                    List<Addressee> invitees) {
        invitees.forEach(invitee ->
            tell(invitee, new InvitationCancelledNotification(invitee.username(), actorName, spaceName, reason, SPACES)));
    }

    @Override
    public void memberJoined(UUID spaceId, String spaceName, String memberName, List<Addressee> recipients) {
        recipients.forEach(reader ->
            tell(reader, new MemberJoinedNotification(reader.username(), memberName, spaceName, members(spaceId))));
    }

    @Override
    public void memberLeft(UUID spaceId, String spaceName, String memberName, List<Addressee> recipients) {
        recipients.forEach(reader ->
            tell(reader, new MemberLeftNotification(reader.username(), memberName, spaceName, false, members(spaceId))));
    }

    @Override
    public void memberLeftNido(UUID spaceId, String spaceName, String memberName, List<Addressee> recipients) {
        recipients.forEach(reader ->
            tell(reader, new MemberLeftNotification(reader.username(), memberName, spaceName, true, members(spaceId))));
    }

    @Override
    public void memberRemoved(UUID spaceId, String spaceName, String actorName, String removedName, List<Addressee> recipients) {
        recipients.forEach(reader -> tell(reader,
            new MemberRemovedNotification(reader.username(), actorName, removedName, spaceName, members(spaceId))));
    }

    @Override
    public void removedFromSpace(String spaceName, String actorName, Addressee removed) {
        tell(removed, new RemovedFromSpaceNotification(removed.username(), actorName, spaceName, SPACES));
    }

    @Override
    public void spaceDeleted(String spaceName, String actorName, List<Addressee> recipients) {
        recipients.forEach(reader ->
            tell(reader, new SpaceDeletedNotification(reader.username(), actorName, spaceName, SPACES)));
    }

    @Override
    public void roleChanged(UUID spaceId, String spaceName, String actorName, Addressee member,
                            SpaceRole previousRole, SpaceRole newRole) {
        tell(member, new RoleChangedNotification(member.username(), actorName, spaceName, previousRole, newRole,
            members(spaceId)));
    }

    @Override
    public void ownershipReceived(UUID spaceId, String spaceName, String actorName, Addressee newOwner) {
        tell(newOwner, new OwnershipReceivedNotification(newOwner.username(), actorName, spaceName, false, members(spaceId)));
    }

    @Override
    public void ownerChanged(UUID spaceId, String spaceName, String actorName, String newOwnerName, List<Addressee> recipients) {
        recipients.forEach(reader -> tell(reader,
            new OwnerChangedNotification(reader.username(), actorName, newOwnerName, spaceName, false, members(spaceId))));
    }

    @Override
    public void ownershipInherited(UUID spaceId, String spaceName, String formerOwnerName, Addressee heir) {
        tell(heir, new OwnershipReceivedNotification(heir.username(), formerOwnerName, spaceName, true, members(spaceId)));
    }

    @Override
    public void ownerSucceeded(UUID spaceId, String spaceName, String formerOwnerName, String heirName,
                               List<Addressee> recipients) {
        recipients.forEach(reader -> tell(reader,
            new OwnerChangedNotification(reader.username(), formerOwnerName, heirName, spaceName, true, members(spaceId))));
    }

    private void tell(Addressee reader, Notification notification) {
        notify.notify(new NotificationRequest(reader.userId(), notification, null));
    }

    private static AppPath members(UUID spaceId) {
        return new AppPath("/s/" + spaceId + "/members");
    }
}
