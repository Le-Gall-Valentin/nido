package com.nido.api.authentication.domain.port.out;

import java.util.UUID;

public interface UserCredentialPort {
    /** Whether the account has a password: an invited account has none until it chooses one. */
    boolean hasCredential(UUID userId);
    void saveCredential(UUID userId, String passwordHash);
    void updatePasswordHash(UUID userId, String newHash);
    void deleteCredential(UUID userId);
}