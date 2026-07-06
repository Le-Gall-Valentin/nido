package com.boilerplate.api.mfa.application.port.in;

import java.util.UUID;

public interface DeleteTotpDataUseCase {
    void deleteUserData(UUID userId);
}