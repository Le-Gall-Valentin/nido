package com.nido.api.space.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.UpdateSpaceCommand;

public interface UpdateSpaceUseCase {
    void update(UpdateSpaceCommand command, SpaceMembership caller);
}
