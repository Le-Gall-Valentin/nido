package com.synapsys.api.identity.domain.model;

import com.synapsys.api.shared.model.Role;
import java.util.UUID;

public record DeleteUserCommand(UUID targetUserId, UUID callerId, Role callerRole) {}