package com.nido.api.space.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.UpdateSpaceUseCase;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.UpdateSpaceCommand;
import com.nido.api.space.domain.port.out.SpaceCommandPort;
import com.nido.api.space.domain.port.out.SpaceRepository;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class UpdateSpaceHandler implements UpdateSpaceUseCase {

    private final SpaceRepository spaceRepository;
    private final SpaceCommandPort spaceCommandPort;

    public UpdateSpaceHandler(SpaceRepository spaceRepository, SpaceCommandPort spaceCommandPort) {
        this.spaceRepository = spaceRepository;
        this.spaceCommandPort = spaceCommandPort;
    }

    @Override
    @Transactional
    public void update(UpdateSpaceCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanManageSpace();
        Space space = spaceRepository.findById(command.spaceId())
            .orElseThrow(SpaceException.SpaceNotFound::new);
        space.ensureShared();
        spaceCommandPort.update(command);
    }
}
