package com.synapsys.api.identity.application.port.in;

import com.synapsys.api.identity.domain.model.ActivateUserCommand;

public interface ActivateUserUseCase {
    void activate(ActivateUserCommand command);
}