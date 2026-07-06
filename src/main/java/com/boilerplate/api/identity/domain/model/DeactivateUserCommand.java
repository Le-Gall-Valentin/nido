package com.boilerplate.api.identity.domain.model;

import com.boilerplate.api.shared.model.Role;
import java.util.UUID;

public record DeactivateUserCommand(UUID targetUserId, UUID callerId, Role callerRole) {}