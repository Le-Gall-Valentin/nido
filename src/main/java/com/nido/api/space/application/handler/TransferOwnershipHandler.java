package com.nido.api.space.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.TransferOwnershipUseCase;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.TransferOwnershipCommand;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;
import com.nido.api.space.domain.port.out.SpaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class TransferOwnershipHandler implements TransferOwnershipUseCase {

    private static final Logger log = LoggerFactory.getLogger(TransferOwnershipHandler.class);

    private final SpaceRepository spaceRepository;
    private final SpaceMembershipPort spaceMembershipPort;

    public TransferOwnershipHandler(SpaceRepository spaceRepository,
                                    SpaceMembershipPort spaceMembershipPort) {
        this.spaceRepository = spaceRepository;
        this.spaceMembershipPort = spaceMembershipPort;
    }

    @Override
    @Transactional
    public void transfer(TransferOwnershipCommand command, SpaceMembership caller) {
        caller.ensureOwner();
        if (command.newOwnerUserId().equals(caller.userId())) {
            throw new SpaceException.SelfManagementForbidden();
        }
        Space space = spaceRepository.findById(command.spaceId())
            .orElseThrow(SpaceException.SpaceNotFound::new);
        space.ensureShared();
        SpaceMembership target = spaceMembershipPort.find(command.spaceId(), command.newOwnerUserId())
            .orElseThrow(SpaceException.MemberNotFound::new);
        if (target.role() != SpaceRole.ADMIN && target.role() != SpaceRole.MEMBER) {
            throw new SpaceException.InsufficientRole();
        }
        // Rétrograder d'abord : l'index unique partiel n'accepte qu'un seul OWNER par espace.
        spaceMembershipPort.changeRole(caller.id(), SpaceRole.ADMIN);
        spaceMembershipPort.changeRole(target.id(), SpaceRole.OWNER);
        log.info("Ownership of space {} transferred from {} to {}",
            space.id(), caller.userId(), target.userId());
    }
}
