package com.nido.api.space.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.GetSpaceUseCase;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceDetailView;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.port.out.SpaceRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class GetSpaceHandler implements GetSpaceUseCase {

    private final SpaceRepository spaceRepository;

    public GetSpaceHandler(SpaceRepository spaceRepository) {
        this.spaceRepository = spaceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SpaceDetailView get(UUID spaceId, SpaceMembership caller) {
        caller.ensureSameSpace(spaceId);
        Space space = spaceRepository.findById(spaceId)
            .orElseThrow(SpaceException.SpaceNotFound::new);
        return new SpaceDetailView(space.id(), space.type(), space.name(), space.description(),
            space.accent(), space.glyph(), caller.role(),
            spaceRepository.countMembers(space.id()));
    }
}
