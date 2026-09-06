package com.nido.api.space.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.GetSpaceEncryptionSaltUseCase;
import com.nido.api.space.domain.port.out.SpaceRepository;

import java.util.UUID;

@ApplicationService
public class GetSpaceEncryptionSaltHandler implements GetSpaceEncryptionSaltUseCase {

    private final SpaceRepository spaceRepository;

    public GetSpaceEncryptionSaltHandler(SpaceRepository spaceRepository) {
        this.spaceRepository = spaceRepository;
    }

    @Override
    public String getEncryptionSalt(UUID spaceId) {
        return spaceRepository.findEncryptionSaltById(spaceId);
    }
}
