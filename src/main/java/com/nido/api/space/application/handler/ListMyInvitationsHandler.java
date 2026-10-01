package com.nido.api.space.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.ListMyInvitationsUseCase;
import com.nido.api.space.domain.model.MemberProfile;
import com.nido.api.space.domain.model.ReceivedInvitationView;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceInvitation;
import com.nido.api.space.domain.port.out.MemberProfilePort;
import com.nido.api.space.domain.port.out.SpaceInvitationPort;
import com.nido.api.space.domain.port.out.SpaceRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationService
public class ListMyInvitationsHandler implements ListMyInvitationsUseCase {

    private final SpaceInvitationPort spaceInvitationPort;
    private final SpaceRepository spaceRepository;
    private final MemberProfilePort memberProfilePort;

    public ListMyInvitationsHandler(SpaceInvitationPort spaceInvitationPort, SpaceRepository spaceRepository,
                                    MemberProfilePort memberProfilePort) {
        this.spaceInvitationPort = spaceInvitationPort;
        this.spaceRepository = spaceRepository;
        this.memberProfilePort = memberProfilePort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceivedInvitationView> listMine(UUID userId) {
        List<SpaceInvitation> invitations = spaceInvitationPort.findPendingForInvitee(userId, Instant.now());
        if (invitations.isEmpty()) {
            return List.of();
        }
        List<UUID> spaceIds = invitations.stream().map(SpaceInvitation::spaceId).distinct().toList();
        Map<UUID, Space> spaceById = spaceRepository.findByIds(spaceIds).stream()
            .collect(Collectors.toMap(Space::id, Function.identity()));
        Map<UUID, String> usernameById = inviterUsernames(invitations);
        return invitations.stream()
            .map(invitation -> toView(invitation, spaceById.get(invitation.spaceId()), usernameById))
            .filter(Objects::nonNull)
            .toList();
    }

    /**
     * Un seul appel pour tous les expéditeurs : pas de N+1 sur le pont vers identity. Un compte
     * anonymisé garde sa ligne mais perd son nom d'utilisateur ; il est simplement absent de la table.
     */
    private Map<UUID, String> inviterUsernames(List<SpaceInvitation> invitations) {
        List<UUID> inviterIds = invitations.stream()
            .map(SpaceInvitation::createdBy)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (inviterIds.isEmpty()) {
            return Map.of();
        }
        return memberProfilePort.findByIds(inviterIds).stream()
            .filter(profile -> profile.username() != null)
            .collect(Collectors.toMap(MemberProfile::userId, MemberProfile::username));
    }

    private static ReceivedInvitationView toView(SpaceInvitation invitation, Space space, Map<UUID, String> usernameById) {
        // Un contexte disparu entre l'émission de l'invitation et sa lecture est ignoré
        // plutôt que de faire échouer la liste entière.
        if (space == null) {
            return null;
        }
        // Map.of() refuse get(null) : une invitation sans expéditeur connu est testée d'abord.
        String invitedBy = invitation.createdBy() == null ? null : usernameById.get(invitation.createdBy());
        return new ReceivedInvitationView(invitation.id(), space.id(), space.name(),
            space.accent(), space.glyph(), invitation.role(), invitation.expiresAt(), invitedBy);
    }
}
