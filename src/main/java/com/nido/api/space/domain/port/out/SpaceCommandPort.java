package com.nido.api.space.domain.port.out;

import com.nido.api.space.domain.model.CreateSharedSpaceCommand;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.UpdateSpaceCommand;

import java.util.UUID;

public interface SpaceCommandPort {
    Space createPersonal(UUID ownerUserId);
    Space createShared(CreateSharedSpaceCommand command);
    void update(UpdateSpaceCommand command);
    void delete(UUID spaceId);
}
