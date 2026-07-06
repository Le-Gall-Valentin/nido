package com.boilerplate.api.authentication.domain.port.out;

import java.util.UUID;

public interface UserCredentialPort {
    void saveCredential(UUID userId, String passwordHash);
    void updatePasswordHash(UUID userId, String newHash);
    void deleteCredential(UUID userId);
}