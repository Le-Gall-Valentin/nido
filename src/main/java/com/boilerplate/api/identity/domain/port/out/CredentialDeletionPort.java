package com.boilerplate.api.identity.domain.port.out;

import java.util.UUID;

public interface CredentialDeletionPort {
    void deleteCredentials(UUID userId);
}