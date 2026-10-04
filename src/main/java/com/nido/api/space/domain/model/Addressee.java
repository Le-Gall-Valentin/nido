package com.nido.api.space.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Somebody a space notification goes to: the account, and the name to greet them by. */
public record Addressee(UUID userId, String username) {

    public Addressee {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(username, "username");
    }
}
