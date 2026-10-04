package com.nido.api.space.domain.port.out;

import com.nido.api.space.domain.model.Addressee;
import com.nido.api.space.domain.model.InvitationCancellation;
import com.nido.api.space.domain.model.SpaceInvitation;
import com.nido.api.space.domain.model.SpaceRole;

import java.util.List;
import java.util.UUID;

/**
 * What happens in a space, told to the people it concerns — on the channels they keep open, once the
 * change is committed. Names are resolved by the caller: this port only says, it never looks up.
 */
public interface SpaceNotificationPort {

    void invitationIssued(SpaceInvitation invitation, String inviteeName, String inviterName, String spaceName);

    /** Pending invitations that stopped being valid, told to their invitees. */
    void invitationCancelled(String spaceName, String actorName, InvitationCancellation reason, List<Addressee> invitees);

    void memberJoined(UUID spaceId, String spaceName, String memberName, List<Addressee> recipients);

    void memberLeft(UUID spaceId, String spaceName, String memberName, List<Addressee> recipients);

    /** A member's account was deleted: they left Nido, and so the space. */
    void memberLeftNido(UUID spaceId, String spaceName, String memberName, List<Addressee> recipients);

    void memberRemoved(UUID spaceId, String spaceName, String actorName, String removedName, List<Addressee> recipients);

    void removedFromSpace(String spaceName, String actorName, Addressee removed);

    void spaceDeleted(String spaceName, String actorName, List<Addressee> recipients);

    void roleChanged(UUID spaceId, String spaceName, String actorName, Addressee member,
                     SpaceRole previousRole, SpaceRole newRole);

    /** @param actorName the former owner who handed it over */
    void ownershipReceived(UUID spaceId, String spaceName, String actorName, Addressee newOwner);

    /** @param actorName the former owner who handed it over */
    void ownerChanged(UUID spaceId, String spaceName, String actorName, String newOwnerName, List<Addressee> recipients);

    /** The owner's account was deleted and the ownership passed on to {@code heir}. */
    void ownershipInherited(UUID spaceId, String spaceName, String formerOwnerName, Addressee heir);

    /** The other members hear who inherited the space whose owner's account was deleted. */
    void ownerSucceeded(UUID spaceId, String spaceName, String formerOwnerName, String heirName, List<Addressee> recipients);
}
