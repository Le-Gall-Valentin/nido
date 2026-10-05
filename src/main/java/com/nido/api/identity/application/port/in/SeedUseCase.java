package com.nido.api.identity.application.port.in;

import com.nido.api.shared.model.Language;

import java.util.Optional;
import java.util.UUID;

public interface SeedUseCase {
    /** Creates the first SUPER_ADMIN, in its language when given; empty when an account already exists. */
    Optional<UUID> seedInitialSuperAdmin(String username, String email, String password, Language language);
}
