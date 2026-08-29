package com.nido.api.kitchen.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;

import java.util.UUID;

public interface RemoveMenuEntryUseCase {
    void remove(UUID entryId, SpaceMembership caller);
}
