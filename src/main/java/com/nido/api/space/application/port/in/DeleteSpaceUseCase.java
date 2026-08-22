package com.nido.api.space.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;

public interface DeleteSpaceUseCase {
    void delete(SpaceMembership caller);
}
