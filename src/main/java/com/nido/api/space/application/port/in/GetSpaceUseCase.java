package com.nido.api.space.application.port.in;

import com.nido.api.space.domain.model.SpaceDetailView;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.UUID;

public interface GetSpaceUseCase {
    SpaceDetailView get(UUID spaceId, SpaceMembership caller);
}
