package com.boilerplate.api.authentication.domain.port.out;

import com.boilerplate.api.authentication.domain.model.UserCredentials;

public interface AccessTokenPort {
    String generate(UserCredentials user);
}