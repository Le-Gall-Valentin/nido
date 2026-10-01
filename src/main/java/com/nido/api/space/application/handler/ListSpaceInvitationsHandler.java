package com.nido.api.space.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.ListSpaceInvitationsUseCase;
import com.nido.api.space.application.service.MemberNames;
import com.nido.api.space.domain.model.SpaceInvitation;
import com.nido.api.space.domain.model.SpaceInvitationView;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.port.out.SpaceInvitationPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationService
public class ListSpaceInvitationsHandler implements ListSpaceInvitationsUseCase {

    private final SpaceInvitationPort spaceInvitationPort;
    private final MemberNames memberNames;

    public ListSpaceInvitationsHandler(SpaceInvitationPort spaceInvitationPort, MemberNames memberNames) {
        this.spaceInvitationPort = spaceInvitationPort;
        this.memberNames = memberNames;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpaceInvitationView> list(SpaceMembership caller) {
        caller.ensureCanManageSpace();
        List<SpaceInvitation> invitations = spaceInvitationPort.findBySpace(caller.spaceId());
        Map<UUID, String> usernameById = memberNames.byId(invitations.stream().map(SpaceInvitation::inviteeId).toList());
        return invitations.stream()
            .map(invitation -> SpaceInvitationView.of(invitation, usernameById.get(invitation.inviteeId())))
            .toList();
    }
}
