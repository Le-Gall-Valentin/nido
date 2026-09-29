package com.nido.api.authentication.domain.model;

import java.util.UUID;

/** Who an account mail goes to, and in which language (a code, or null when never recorded). */
public record AccountContact(UUID userId, String username, String email, String language) {

    public static AccountContact of(UserProfile profile) {
        return new AccountContact(profile.id(), profile.username(), profile.email(), profile.language());
    }

    public static AccountContact of(UserCredentials credentials) {
        return new AccountContact(credentials.id(), credentials.username(), credentials.email(), credentials.language());
    }
}
