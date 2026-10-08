package com.nido.api.identity.domain.model;

import com.nido.api.shared.model.Language;
import com.nido.api.shared.model.Role;
import com.nido.api.shared.model.TwoFactorMethod;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserSelfView(
    UUID id,
    String username,
    String email,
    Role role,
    Instant createdAt,
    Set<TwoFactorMethod> twoFactorMethods,
    Language language
) {}