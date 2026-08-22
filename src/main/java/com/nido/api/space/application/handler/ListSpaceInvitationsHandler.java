package com.nido.api.space.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.ListSpaceInvitationsUseCase;
import com.nido.api.space.domain.model.SpaceInvitation;
import com.nido.api.space.domain.model.SpaceInvitationView;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.port.out.SpaceInvitationPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@ApplicationService
public class ListSpaceInvitationsHandler implements ListSpaceInvitationsUseCase {

    private final SpaceInvitationPort spaceInvitationPort;

    public ListSpaceInvitationsHandler(SpaceInvitationPort spaceInvitationPort) {
        this.spaceInvitationPort = spaceInvitationPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpaceInvitationView> list(SpaceMembership caller) {
        caller.ensureCanManageSpace();
        return spaceInvitationPort.findBySpace(caller.spaceId()).stream()
            .map(ListSpaceInvitationsHandler::toView)
            .toList();
    }

    private static SpaceInvitationView toView(SpaceInvitation invitation) {
        return new SpaceInvitationView(invitation.id(), invitation.email(), invitation.role(),
            invitation.code(), invitation.status(), invitation.expiresAt(), invitation.createdAt());
    }
}
