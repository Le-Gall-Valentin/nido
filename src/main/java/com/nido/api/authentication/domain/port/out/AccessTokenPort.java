package com.nido.api.authentication.domain.port.out;

import com.nido.api.authentication.domain.model.UserCredentials;

public interface AccessTokenPort {
    String generate(UserCredentials user);
}