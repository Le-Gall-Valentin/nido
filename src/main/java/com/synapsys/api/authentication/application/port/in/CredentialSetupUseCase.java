package com.synapsys.api.authentication.application.port.in;

import java.util.UUID;

public interface CredentialSetupUseCase {
    void setup(UUID userId, String rawPassword);
}