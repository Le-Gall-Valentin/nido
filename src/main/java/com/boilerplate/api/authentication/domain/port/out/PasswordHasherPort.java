package com.boilerplate.api.authentication.domain.port.out;

public interface PasswordHasherPort {
    String hash(String raw);
}