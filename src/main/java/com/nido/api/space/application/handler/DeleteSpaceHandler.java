package com.nido.api.space.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.service.SpaceNotifier;
import com.nido.api.space.application.port.in.DeleteSpaceUseCase;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.port.out.SpaceCommandPort;
import com.nido.api.space.domain.port.out.SpaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class DeleteSpaceHandler implements DeleteSpaceUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteSpaceHandler.class);

    private final SpaceRepository spaceRepository;
    private final SpaceCommandPort spaceCommandPort;
    private final SpaceNotifier spaceNotifier;

    public DeleteSpaceHandler(SpaceRepository spaceRepository, SpaceCommandPort spaceCommandPort,
                              SpaceNotifier spaceNotifier) {
        this.spaceRepository = spaceRepository;
        this.spaceCommandPort = spaceCommandPort;
        this.spaceNotifier = spaceNotifier;
    }

    @Override
    @Transactional
    public void delete(SpaceMembership caller) {
        caller.ensureOwner();
        Space space = spaceRepository.findById(caller.spaceId())
            .orElseThrow(SpaceException.SpaceNotFound::new);
        space.ensureShared();
        // Before the deletion, which takes the memberships with it: afterwards there is nobody left to tell.
        spaceNotifier.spaceDeleted(space.id(), caller.userId());
        spaceCommandPort.delete(space.id());
        log.info("Shared space {} deleted by its owner {}", space.id(), caller.userId());
    }
}
