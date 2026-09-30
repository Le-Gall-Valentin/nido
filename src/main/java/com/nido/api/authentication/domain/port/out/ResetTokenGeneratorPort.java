package com.nido.api.authentication.domain.port.out;

public interface ResetTokenGeneratorPort {
    /** A new unguessable token, safe to put in a URL. */
    String newToken();
}
