package com.nido.api.shared.security;

/**
 * A value read from the database and refused because it failed its integrity check — moved, copied or altered there.
 * Carries the place of the value, never the value. Shared so that a reader that degrades gracefully, such as the
 * dashboard, can still tell it from an ordinary failure and raise it as loudly.
 */
public abstract class StoredValueRejected extends IllegalStateException {

    protected StoredValueRejected(String message) {
        super(message);
    }
}
