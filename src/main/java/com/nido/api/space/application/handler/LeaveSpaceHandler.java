package com.nido.api.space.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.service.SpaceDepartures;
import com.nido.api.space.application.service.SpaceNotifier;
import com.nido.api.space.application.port.in.LeaveSpaceUseCase;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;
import com.nido.api.space.domain.port.out.SpaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class LeaveSpaceHandler implements LeaveSpaceUseCase {

    private static final Logger log = LoggerFactory.getLogger(LeaveSpaceHandler.class);

    private final SpaceRepository spaceRepository;
    private final SpaceMembershipPort spaceMembershipPort;
    private final SpaceNotifier spaceNotifier;
    private final SpaceDepartures departures;

    public LeaveSpaceHandler(SpaceRepository spaceRepository, SpaceMembershipPort spaceMembershipPort,
                             SpaceNotifier spaceNotifier, SpaceDepartures departures) {
        this.spaceRepository = spaceRepository;
        this.spaceMembershipPort = spaceMembershipPort;
        this.spaceNotifier = spaceNotifier;
        this.departures = departures;
    }

    @Override
    @Transactional
    public void leave(SpaceMembership caller) {
        Space space = spaceRepository.findById(caller.spaceId())
            .orElseThrow(SpaceException.SpaceNotFound::new);
        space.ensureShared();
        if (caller.isOwner()) {
            throw new SpaceException.LastOwnerCannotLeave();
        }
        spaceMembershipPort.remove(caller.id());
        departures.departed(caller.spaceId(), caller.userId());
        log.info("User {} left space {}", caller.userId(), space.id());
        spaceNotifier.memberLeft(space.id(), caller.userId());
    }
}
