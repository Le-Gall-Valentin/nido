package com.nido.api.space.domain.port.out;

import com.nido.api.space.domain.model.SpaceInvitation;

public interface InvitationNotificationPort {
    /**
     * Tells the invitee, on the channels they keep open, inside the inviting transaction — an invitation
     * that does not commit tells nobody.
     */
    void invitationIssued(SpaceInvitation invitation, String inviteeName, String inviterName, String spaceName);
}
