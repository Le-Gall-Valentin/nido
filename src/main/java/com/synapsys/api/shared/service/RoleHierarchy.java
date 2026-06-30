package com.synapsys.api.shared.service;

import com.synapsys.api.shared.model.Role;

public final class RoleHierarchy {
    private RoleHierarchy() {}

    // SUPER_ADMIN cannot manage another SUPER_ADMIN — accounts at that level are
    // indestructible by design and must be managed out-of-band (DB / ops).
    public static boolean canManage(Role callerRole, Role targetRole) {
        return switch (callerRole) {
            case SUPER_ADMIN -> targetRole == Role.ADMIN || targetRole == Role.USER;
            case ADMIN       -> targetRole == Role.USER;
            default          -> false;
        };
    }
}