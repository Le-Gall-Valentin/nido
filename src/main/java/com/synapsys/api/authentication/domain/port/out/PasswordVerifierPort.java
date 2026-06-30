package com.synapsys.api.authentication.domain.port.out;

public interface PasswordVerifierPort {
    boolean matches(String rawPassword, String hash);
}