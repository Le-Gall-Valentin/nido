package com.nido.api.space.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.port.out.SpaceRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

@ApplicationService
public class GetSpaceTodayHandler implements GetSpaceTodayUseCase {

    private final SpaceRepository spaceRepository;
    private final Clock clock;

    public GetSpaceTodayHandler(SpaceRepository spaceRepository, Clock clock) {
        this.spaceRepository = spaceRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDate today(UUID spaceId) {
        Space space = spaceRepository.findById(spaceId).orElseThrow(SpaceException.SpaceNotFound::new);
        return LocalDate.now(clock.withZone(space.timezone()));
    }
}
