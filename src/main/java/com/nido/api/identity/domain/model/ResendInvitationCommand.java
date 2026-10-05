package com.nido.api.identity.domain.model;

import com.nido.api.shared.model.Role;

import java.util.UUID;

public record ResendInvitationCommand(UUID targetUserId, UUID callerId, Role callerRole) {
}
