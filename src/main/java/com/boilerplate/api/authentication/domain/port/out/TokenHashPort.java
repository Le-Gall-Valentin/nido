package com.boilerplate.api.authentication.domain.port.out;

public interface TokenHashPort {
    String hash(String rawValue);
}