package com.synapsys.api.identity.domain.model;

import com.synapsys.api.shared.model.Role;
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