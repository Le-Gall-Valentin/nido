package com.nido.api.space.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.CreatePersonalSpaceUseCase;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.port.out.SpaceCommandPort;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;
import com.nido.api.space.domain.port.out.SpaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

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
        Space space;
        try {
            space = spaceCommandPort.createPersonal(userId);
        } catch (SpaceException.PersonalSpaceAlreadyExists e) {
            return spaceRepository.findPersonalOwnedBy(userId)
                .orElseThrow(SpaceException.SpaceNotFound::new)
                .id();
        }
        spaceMembershipPort.add(space.id(), userId, SpaceRole.OWNER);
        log.info("Personal space {} created for user {}", space.id(), userId);
        return space.id();
    }
}
