package com.nido.api.space.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.HandleUserDeletionUseCase;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.port.out.SpaceCommandPort;
import com.nido.api.space.domain.port.out.SpaceInvitationPort;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;
import com.nido.api.space.domain.port.out.SpaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@ApplicationService
public class HandleUserDeletionHandler implements HandleUserDeletionUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandleUserDeletionHandler.class);

    private final SpaceRepository spaceRepository;
    private final SpaceCommandPort spaceCommandPort;
    private final SpaceMembershipPort spaceMembershipPort;
    private final SpaceInvitationPort spaceInvitationPort;

    public HandleUserDeletionHandler(SpaceRepository spaceRepository,
                                     SpaceCommandPort spaceCommandPort,
                                     SpaceMembershipPort spaceMembershipPort,
                                     SpaceInvitationPort spaceInvitationPort) {
        this.spaceRepository = spaceRepository;
        this.spaceCommandPort = spaceCommandPort;
        this.spaceMembershipPort = spaceMembershipPort;
        this.spaceInvitationPort = spaceInvitationPort;
    }

    @Override
    @Transactional
    public void handleUserDeletion(UUID userId, String email) {
        for (SpaceMembership membership : spaceMembershipPort.findByUser(userId)) {
            Optional<Space> maybeSpace = spaceRepository.findById(membership.spaceId());
            if (maybeSpace.isEmpty()) {
                continue;
            }
            Space space = maybeSpace.get();
            if (space.isPersonal()) {
                // la suppression de l'espace emporte ses adhésions (ON DELETE CASCADE)
                spaceCommandPort.delete(space.id());
                continue;
            }
            if (!membership.isOwner()) {
                spaceMembershipPort.remove(membership.id());
                continue;
            }
            Optional<SpaceMembership> successor = spaceMembershipPort.findSuccessor(space.id(), userId);
            if (successor.isEmpty()) {
                spaceCommandPort.delete(space.id());
                log.info("Space {} deleted: its owner {} was deleted and no successor remained",
                    space.id(), userId);
                continue;
            }
            // Retirer l'ancien propriétaire d'abord : un seul OWNER à la fois.
            spaceMembershipPort.remove(membership.id());
            spaceMembershipPort.changeRole(successor.get().id(), SpaceRole.OWNER);
            log.info("Ownership of space {} passed to {} after the deletion of {}",
                space.id(), successor.get().userId(), userId);
        }
        if (email != null) {
            int deleted = spaceInvitationPort.deleteAllForEmail(email);
            if (deleted > 0) {
                log.info("{} invitation(s) addressed to the deleted user {} were removed", deleted, userId);
            }
        }
    }
}
