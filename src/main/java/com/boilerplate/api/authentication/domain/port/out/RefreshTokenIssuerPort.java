package com.boilerplate.api.authentication.domain.port.out;

import com.boilerplate.api.authentication.domain.model.UserCredentials;

public interface RefreshTokenIssuerPort {
    String generate(UserCredentials user, int expiryDays);
}