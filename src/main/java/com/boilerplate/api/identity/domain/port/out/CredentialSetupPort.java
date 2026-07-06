package com.boilerplate.api.identity.domain.port.out;

import java.util.UUID;

public interface CredentialSetupPort {
    void setup(UUID userId, String rawPassword);
}