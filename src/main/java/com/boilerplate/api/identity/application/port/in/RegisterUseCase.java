package com.boilerplate.api.identity.application.port.in;

import com.boilerplate.api.identity.domain.model.RegisterCommand;
import com.boilerplate.api.identity.domain.model.User;
import com.boilerplate.api.shared.model.Role;

public interface RegisterUseCase {
    User register(RegisterCommand command, Role callerRole);
}