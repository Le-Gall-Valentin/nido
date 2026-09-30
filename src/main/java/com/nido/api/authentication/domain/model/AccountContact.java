package com.nido.api.authentication.domain.model;

import com.nido.api.shared.model.Language;
import java.util.UUID;

/** Who an account mail goes to, and in which language (null when the account never recorded one). */
public record AccountContact(UUID userId, String username, String email, Language language) {

    public static AccountContact of(UserProfile profile) {
        return new AccountContact(profile.id(), profile.username(), profile.email(), profile.language());
    }

    public static AccountContact of(UserCredentials credentials) {
        return new AccountContact(credentials.id(), credentials.username(), credentials.email(), credentials.language());
    }
}
