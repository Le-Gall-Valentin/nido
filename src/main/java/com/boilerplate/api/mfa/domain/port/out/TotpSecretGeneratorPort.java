package com.boilerplate.api.mfa.domain.port.out;

public interface TotpSecretGeneratorPort {
    String generateSecret();
}