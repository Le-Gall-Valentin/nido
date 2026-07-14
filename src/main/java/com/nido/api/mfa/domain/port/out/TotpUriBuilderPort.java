package com.nido.api.mfa.domain.port.out;

public interface TotpUriBuilderPort {
    String buildOtpauthUri(String secret, String email);
}