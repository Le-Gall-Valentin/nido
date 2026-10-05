package com.nido.api.authentication.domain.port.out;

import java.util.Optional;

/** The address people reach this installation at, when the installation knows it. */
public interface PublicUrlPort {
    /** Without a trailing '/': {@code https://nido.example}. */
    Optional<String> publicUrl();
}
