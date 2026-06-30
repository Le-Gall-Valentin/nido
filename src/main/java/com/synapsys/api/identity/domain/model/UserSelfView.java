package com.synapsys.api.identity.domain.model;

import com.synapsys.api.shared.model.Role;
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