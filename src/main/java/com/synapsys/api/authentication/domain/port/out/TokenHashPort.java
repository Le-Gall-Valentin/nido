package com.synapsys.api.authentication.domain.port.out;

public interface TokenHashPort {
    String hash(String rawValue);
}