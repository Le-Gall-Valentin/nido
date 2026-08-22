package com.nido.api.space.application.port.in;

import com.nido.api.space.domain.model.SpaceDetailView;
import com.nido.api.space.domain.model.SpaceMembership;

public interface GetSpaceUseCase {
    SpaceDetailView get(SpaceMembership membership);
}
