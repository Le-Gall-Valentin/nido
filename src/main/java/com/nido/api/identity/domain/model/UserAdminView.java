package com.nido.api.identity.domain.model;

import com.nido.api.shared.model.Role;
import com.nido.api.shared.model.TwoFactorMethod;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserAdminView(
    UUID id,
    String username,
    String email,
    Role role,
    boolean isActive,
    Instant createdAt,
    /** On, paused ones included. */
    Set<TwoFactorMethod> twoFactorMethods,
    /** Null once the account chose its password. */
    InvitationState invitation
) {}