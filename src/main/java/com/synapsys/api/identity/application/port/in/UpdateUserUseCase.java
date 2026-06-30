package com.synapsys.api.identity.application.port.in;

import com.synapsys.api.identity.domain.model.UpdateUserCommand;

public interface UpdateUserUseCase {
    void update(UpdateUserCommand command);
}