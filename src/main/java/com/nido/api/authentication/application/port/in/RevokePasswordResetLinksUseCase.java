package com.nido.api.authentication.application.port.in;

import java.util.UUID;

/** Voids every reset link an account was sent — for identity, when the address they went to changes. */
public interface RevokePasswordResetLinksUseCase {
    void revokeFor(UUID userId);
}
