package com.synapsys.api.identity.domain.model;

import com.synapsys.api.shared.model.Role;
import com.synapsys.api.shared.service.RoleHierarchy;
import java.time.Instant;
import java.util.UUID;

public record User(
    UUID id,
    String username,
    String email,
    Role role,
    boolean isActive,
    Instant createdAt
) {
    public void ensureActive() {
        if (!isActive) throw new IdentityException.UserAlreadyInactive();
    }

    public void ensureInactive() {
        if (isActive) throw new IdentityException.UserAlreadyActive();
    }

    public void ensureCanBeDeactivatedBy(Role callerRole) { ensureCanBeManagedBy(callerRole); }

    public void ensureCanBeActivatedBy(Role callerRole) { ensureCanBeManagedBy(callerRole); }

    public void ensureCanBeDeletedBy(Role callerRole) { ensureCanBeManagedBy(callerRole); }

    public void ensureCanBeUpdatedBy(Role callerRole) { ensureCanBeManagedBy(callerRole); }

    public void ensureRoleCanBeAssignedBy(Role callerRole, Role newRole) {
        if (!RoleHierarchy.canManage(callerRole, newRole)) {
            throw new IdentityException.InsufficientPermissions();
        }
    }

    public void ensureRoleNotAlreadyAssigned(Role newRole) {
        if (this.role == newRole) {
            throw new IdentityException.RoleAlreadyAssigned();
        }
    }

    private void ensureCanBeManagedBy(Role callerRole) {
        if (!RoleHierarchy.canManage(callerRole, this.role)) {
            throw new IdentityException.InsufficientPermissions();
        }
    }
}