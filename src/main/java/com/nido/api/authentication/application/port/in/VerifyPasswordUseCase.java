package com.nido.api.authentication.application.port.in;

import java.util.UUID;

/** Whether a password is an account's current one — for identity, which must not see the hash. */
public interface VerifyPasswordUseCase {
    boolean verify(UUID userId, String rawPassword);
}
