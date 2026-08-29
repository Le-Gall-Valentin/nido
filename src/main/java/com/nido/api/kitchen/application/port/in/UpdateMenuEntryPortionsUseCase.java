package com.nido.api.kitchen.application.port.in;

import com.nido.api.kitchen.domain.model.UpdateMenuEntryPortionsCommand;
import com.nido.api.space.domain.model.SpaceMembership;

public interface UpdateMenuEntryPortionsUseCase {
    void updatePortions(UpdateMenuEntryPortionsCommand command, SpaceMembership caller);
}
