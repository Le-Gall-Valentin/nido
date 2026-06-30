package com.synapsys.api.authentication.domain.model;

import com.synapsys.api.shared.model.Role;
import java.time.Instant;
import java.util.UUID;

public record UserCredentials(
    UUID id,
    String username,
    String email,
    String passwordHash,
    boolean isActive,
    Role role,
    Instant createdAt
) {
    @Override
    public String toString() {
        return "UserCredentials[id=" + id + ", username=" + username +
               ", email=" + email + ", isActive=" + isActive +
               ", role=" + role + "]";
    }
}