package com.synapsys.api.identity.application.port.in;

import com.synapsys.api.identity.domain.model.RegisterCommand;
import com.synapsys.api.identity.domain.model.User;
import com.synapsys.api.shared.model.Role;

public interface RegisterUseCase {
    User register(RegisterCommand command, Role callerRole);
}