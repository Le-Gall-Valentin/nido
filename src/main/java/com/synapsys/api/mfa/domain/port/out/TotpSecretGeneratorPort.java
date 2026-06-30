package com.synapsys.api.mfa.domain.port.out;

public interface TotpSecretGeneratorPort {
    String generateSecret();
}