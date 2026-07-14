package com.nido.api.identity.application.port.in;

import com.nido.api.identity.domain.model.RegisterCommand;
import com.nido.api.identity.domain.model.User;
import com.nido.api.shared.model.Role;

public interface RegisterUseCase {
    User register(RegisterCommand command, Role callerRole);
}