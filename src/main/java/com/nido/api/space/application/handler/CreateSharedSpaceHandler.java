package com.nido.api.space.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.CreateSharedSpaceUseCase;
import com.nido.api.space.domain.model.CreateSharedSpaceCommand;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.port.out.SpaceCommandPort;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class CreateSharedSpaceHandler implements CreateSharedSpaceUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateSharedSpaceHandler.class);

    private final SpaceCommandPort spaceCommandPort;
    private final SpaceMembershipPort spaceMembershipPort;

    public CreateSharedSpaceHandler(SpaceCommandPort spaceCommandPort,
                                    SpaceMembershipPort spaceMembershipPort) {
        this.spaceCommandPort = spaceCommandPort;
        this.spaceMembershipPort = spaceMembershipPort;
    }

    @Override
    @Transactional
    public Space create(CreateSharedSpaceCommand command) {
        Space space = spaceCommandPort.createShared(command);
        spaceMembershipPort.add(space.id(), command.creatorUserId(), SpaceRole.OWNER);
        log.info("Shared space {} created by user {}", space.id(), command.creatorUserId());
        return space;
    }
}
