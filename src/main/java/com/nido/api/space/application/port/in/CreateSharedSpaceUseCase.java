package com.nido.api.space.application.port.in;

import com.nido.api.space.domain.model.CreateSharedSpaceCommand;
import com.nido.api.space.domain.model.Space;

public interface CreateSharedSpaceUseCase {
    Space create(CreateSharedSpaceCommand command);
}
