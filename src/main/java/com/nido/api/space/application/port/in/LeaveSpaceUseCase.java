package com.nido.api.space.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;

public interface LeaveSpaceUseCase {
    void leave(SpaceMembership caller);
}
