package com.boilerplate.api.authentication.domain.model;

import com.boilerplate.api.shared.model.Role;
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