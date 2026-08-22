package com.nido.api.space.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.DeleteEmptySpaceUseCase;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.port.out.SpaceCommandPort;
import com.nido.api.space.domain.port.out.SpaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class DeleteEmptySpaceHandler implements DeleteEmptySpaceUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteEmptySpaceHandler.class);

    private final SpaceRepository spaceRepository;
    private final SpaceCommandPort spaceCommandPort;

    public DeleteEmptySpaceHandler(SpaceRepository spaceRepository, SpaceCommandPort spaceCommandPort) {
        this.spaceRepository = spaceRepository;
        this.spaceCommandPort = spaceCommandPort;
    }

    @Override
    @Transactional
    public void delete(UUID spaceId, UUID callerId) {
        Space space = spaceRepository.findById(spaceId)
            .orElseThrow(SpaceException.SpaceNotFound::new);
        if (spaceRepository.countMembers(spaceId) > 0) {
            throw new SpaceException.SpaceNotEmpty();
        }
        spaceCommandPort.delete(space.id());
        log.warn("Platform admin {} deleted the member-less space {}", callerId, spaceId);
    }
}
