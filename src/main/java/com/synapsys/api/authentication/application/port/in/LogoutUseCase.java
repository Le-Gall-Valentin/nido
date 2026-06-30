package com.synapsys.api.authentication.application.port.in;

public interface LogoutUseCase {
    void logout(String rawRefreshToken);
}