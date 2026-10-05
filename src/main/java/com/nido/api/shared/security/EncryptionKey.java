package com.nido.api.shared.security;

import java.util.Objects;

/**
 * The installation's master encryption key: with a salt, every encryptor of the application derives
 * its own key from it. It lives outside the database — environment, secret file or data directory —
 * so that a database dump alone decrypts nothing. Never printed.
 */
public record EncryptionKey(String value) {

    public EncryptionKey {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("An encryption key cannot be blank");
        }
    }

    @Override
    public String toString() {
        return "EncryptionKey[***]";
    }
}
