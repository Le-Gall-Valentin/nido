package com.nido.api.identity.domain.model;

import com.nido.api.shared.model.Role;
import com.nido.api.shared.model.TwoFactorMethod;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/** @param methods the methods the administrator ticked — at least one */
public record AdminResetTwoFactorCommand(UUID targetUserId, UUID callerId, Role callerRole, Set<TwoFactorMethod> methods) {
    public AdminResetTwoFactorCommand {
        if (methods == null || methods.isEmpty()) {
            throw new IllegalArgumentException("A reset names at least one method");
        }
        methods = EnumSet.copyOf(methods);
    }
}
