package com.boilerplate.api.identity.application.port.in;

import com.boilerplate.api.identity.domain.model.UpdateUserCommand;

public interface UpdateUserUseCase {
    void update(UpdateUserCommand command);
}