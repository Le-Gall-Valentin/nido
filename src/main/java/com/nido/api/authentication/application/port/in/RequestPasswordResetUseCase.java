package com.nido.api.authentication.application.port.in;

public interface RequestPasswordResetUseCase {
    /** Sends a reset link if an active account answers to this username or address; says nothing either way. */
    void request(String identifier);
}
