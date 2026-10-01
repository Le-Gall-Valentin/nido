package com.nido.api.space.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.ListSpaceInvitationsUseCase;
import com.nido.api.space.domain.model.MemberProfile;
import com.nido.api.space.domain.model.SpaceInvitation;
import com.nido.api.space.domain.model.SpaceInvitationView;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.port.out.MemberProfilePort;
import com.nido.api.space.domain.port.out.SpaceInvitationPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationService
public class ListSpaceInvitationsHandler implements ListSpaceInvitationsUseCase {

    private final SpaceInvitationPort spaceInvitationPort;
    private final MemberProfilePort memberProfilePort;

    public ListSpaceInvitationsHandler(SpaceInvitationPort spaceInvitationPort, MemberProfilePort memberProfilePort) {
        this.spaceInvitationPort = spaceInvitationPort;
        this.memberProfilePort = memberProfilePort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpaceInvitationView> list(SpaceMembership caller) {
        caller.ensureCanManageSpace();
        List<SpaceInvitation> invitations = spaceInvitationPort.findBySpace(caller.spaceId());
        Map<UUID, String> usernameById = inviteeUsernames(invitations);
        return invitations.stream()
            .map(invitation -> toView(invitation, usernameById.get(invitation.inviteeId())))
            .toList();
    }

    /**
     * Un seul appel pour tous les invités : pas de N+1 sur le pont vers identity. Un compte qui n'est
     * plus résoluble est simplement absent de la table.
     */
    private Map<UUID, String> inviteeUsernames(List<SpaceInvitation> invitations) {
        List<UUID> inviteeIds = invitations.stream().map(SpaceInvitation::inviteeId).distinct().toList();
        if (inviteeIds.isEmpty()) {
            return Map.of();
        }
        return memberProfilePort.findByIds(inviteeIds).stream()
            .filter(profile -> profile.username() != null)
            .collect(Collectors.toMap(MemberProfile::userId, MemberProfile::username));
    }

    private static SpaceInvitationView toView(SpaceInvitation invitation, String username) {
        return new SpaceInvitationView(invitation.id(), username, invitation.role(),
            invitation.code(), invitation.status(), invitation.expiresAt(), invitation.createdAt());
    }
}
