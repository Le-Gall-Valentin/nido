package com.nido.api.identity.application.port.in;

import com.nido.api.identity.domain.model.RegisterCommand;
import com.nido.api.identity.domain.model.RegisteredAccount;
import com.nido.api.shared.model.Role;

import java.util.UUID;

public interface RegisterUseCase {
    /** Creates the account and invites it: it chooses its own password with the link. */
    RegisteredAccount register(RegisterCommand command, UUID callerId, Role callerRole);
}
