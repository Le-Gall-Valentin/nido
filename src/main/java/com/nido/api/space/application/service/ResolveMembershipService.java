package com.nido.api.space.application.service;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.ResolveMembershipUseCase;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class ResolveMembershipService implements ResolveMembershipUseCase {

    private final SpaceMembershipPort spaceMembershipPort;

    public ResolveMembershipService(SpaceMembershipPort spaceMembershipPort) {
        this.spaceMembershipPort = spaceMembershipPort;
    }

    @Override
    @Transactional(readOnly = true)
    public SpaceMembership resolve(UUID spaceId, UUID userId) {
        return spaceMembershipPort.find(spaceId, userId)
            .orElseThrow(SpaceException.NotAMember::new);
    }
}
