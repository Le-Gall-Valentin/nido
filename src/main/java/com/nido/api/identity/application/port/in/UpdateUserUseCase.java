package com.nido.api.identity.application.port.in;

import com.nido.api.identity.domain.model.UpdateUserCommand;

public interface UpdateUserUseCase {
    void update(UpdateUserCommand command);
}