package com.nido.api.identity.domain.model;

import java.util.Optional;

/**
 * What a person typed to name an account — at sign-in, in "forgot password", when inviting. With an '@'
 * it is an email address; without one, a username. The two never overlap: a username holds no '@'
 * ({@link Username}). Not a technical id.
 */
public sealed interface AccountIdentifier permits AccountIdentifier.ByUsername, AccountIdentifier.ByEmail {

    /** A typed username, as typed: it is looked up, not created, so a name too short simply finds nobody. */
    record ByUsername(String value) implements AccountIdentifier {}

    record ByEmail(EmailAddress address) implements AccountIdentifier {}

    /** Surrounding spaces are dropped; a blank value names nobody. */
    static Optional<AccountIdentifier> parse(String typed) {
        if (typed == null || typed.isBlank()) {
            return Optional.empty();
        }
        String stripped = typed.strip();
        return Optional.of(stripped.indexOf('@') >= 0
            ? new ByEmail(new EmailAddress(stripped))
            : new ByUsername(stripped));
    }
}
