package com.boilerplate.api.identity.domain.model;

import com.boilerplate.api.shared.model.Role;
import java.time.Instant;
import java.util.UUID;

public record UserSelfView(
    UUID id,
    String username,
    String email,
    Role role,
    Instant createdAt,
    boolean totpEnabled
) {}