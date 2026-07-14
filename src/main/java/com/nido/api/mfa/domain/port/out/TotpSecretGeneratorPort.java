package com.nido.api.mfa.domain.port.out;

public interface TotpSecretGeneratorPort {
    String generateSecret();
}