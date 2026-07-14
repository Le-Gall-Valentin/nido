package com.nido.api.authentication.domain.port.out;

public interface TokenHashPort {
    String hash(String rawValue);
}