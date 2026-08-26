package com.nido.api.space.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.CreatePersonalSpaceUseCase;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.port.out.SpaceCommandPort;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;
import com.nido.api.space.domain.port.out.SpaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@ApplicationService
public class CreatePersonalSpaceHandler implements CreatePersonalSpaceUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreatePersonalSpaceHandler.class);

    private final SpaceRepository spaceRepository;
    private final SpaceCommandPort spaceCommandPort;
    private final SpaceMembershipPort spaceMembershipPort;

    public CreatePersonalSpaceHandler(SpaceRepository spaceRepository,
                                      SpaceCommandPort spaceCommandPort,
                                      SpaceMembershipPort spaceMembershipPort) {
        this.spaceRepository = spaceRepository;
        this.spaceCommandPort = spaceCommandPort;
        this.spaceMembershipPort = spaceMembershipPort;
    }

    @Override
    @Transactional
    public UUID createFor(UUID userId) {
        Optional<Space> existing = spaceRepository.findPersonalOwnedBy(userId);
        if (existing.isPresent()) {
            return existing.get().id();
        }
        // Une violation de uq_spaces_personal_owner ici signifie une vraie course :
        // on la laisse remonter, la session Hibernate n'étant plus utilisable après le flush.
        Space space = spaceCommandPort.createPersonal(userId);
        spaceMembershipPort.add(space.id(), userId, SpaceRole.OWNER);
        log.info("Personal space {} created for user {}", space.id(), userId);
        return space.id();
    }
}
