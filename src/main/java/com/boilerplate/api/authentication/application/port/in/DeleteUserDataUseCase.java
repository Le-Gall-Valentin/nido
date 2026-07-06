package com.boilerplate.api.authentication.application.port.in;

import java.util.UUID;

public interface DeleteUserDataUseCase {
    void delete(UUID userId);
}