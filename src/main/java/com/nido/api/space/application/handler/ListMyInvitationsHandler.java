package com.nido.api.space.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.ListMyInvitationsUseCase;
import com.nido.api.space.domain.model.ReceivedInvitationView;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceInvitation;
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

    public ListMyInvitationsHandler(SpaceInvitationPort spaceInvitationPort, SpaceRepository spaceRepository) {
        this.spaceInvitationPort = spaceInvitationPort;
        this.spaceRepository = spaceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceivedInvitationView> listMine(String email) {
        List<SpaceInvitation> invitations = spaceInvitationPort.findPendingForEmail(email, Instant.now());
        if (invitations.isEmpty()) {
            return List.of();
        }
        List<UUID> spaceIds = invitations.stream().map(SpaceInvitation::spaceId).distinct().toList();
        Map<UUID, Space> spaceById = spaceRepository.findByIds(spaceIds).stream()
            .collect(Collectors.toMap(Space::id, Function.identity()));
        return invitations.stream()
            .map(invitation -> toView(invitation, spaceById.get(invitation.spaceId())))
            .filter(Objects::nonNull)
            .toList();
    }

    private static ReceivedInvitationView toView(SpaceInvitation invitation, Space space) {
        // Un contexte disparu entre l'émission de l'invitation et sa lecture est ignoré
        // plutôt que de faire échouer la liste entière.
        if (space == null) {
            return null;
        }
        return new ReceivedInvitationView(invitation.id(), space.id(), space.name(),
            space.accent(), space.glyph(), invitation.role(), invitation.expiresAt());
    }
}
