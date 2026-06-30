package com.synapsys.api.authentication.domain.port.out;

public interface PasswordHasherPort {
    String hash(String raw);
}