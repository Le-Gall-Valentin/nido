package com.boilerplate.api.identity.domain.model;

import com.boilerplate.api.shared.model.Role;
import java.time.Instant;
import java.util.UUID;

public record UserAdminView(
    UUID id,
    String username,
    String email,
    Role role,
    boolean isActive,
    Instant createdAt,
    boolean totpEnabled
) {}