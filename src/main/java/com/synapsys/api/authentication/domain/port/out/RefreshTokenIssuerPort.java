package com.synapsys.api.authentication.domain.port.out;

import com.synapsys.api.authentication.domain.model.UserCredentials;

public interface RefreshTokenIssuerPort {
    String generate(UserCredentials user, int expiryDays);
}