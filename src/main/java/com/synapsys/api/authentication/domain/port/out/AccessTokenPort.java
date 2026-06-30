package com.synapsys.api.authentication.domain.port.out;

import com.synapsys.api.authentication.domain.model.UserCredentials;

public interface AccessTokenPort {
    String generate(UserCredentials user);
}