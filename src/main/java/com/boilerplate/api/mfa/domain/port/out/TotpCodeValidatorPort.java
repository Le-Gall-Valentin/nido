package com.boilerplate.api.mfa.domain.port.out;

public interface TotpCodeValidatorPort {
    boolean isValid(String secret, String code);
}