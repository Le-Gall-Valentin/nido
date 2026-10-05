package com.nido.api.instance.domain.model;

import java.util.Optional;

/**
 * What a start does about the encryption key, from the key it was given and what the database
 * remembers. A key is generated only where nothing was ever encrypted: no fingerprint, and the setup
 * not done — an installation is marked set up as soon as it has accounts (migration 064, the seed,
 * the setup screen), so this is the same as "no account" without asking the identity context.
 */
public sealed interface EncryptionKeyDecision {

    int MIN_LENGTH = 32;

    record Use(String key) implements EncryptionKeyDecision {
        @Override public String toString() { return "Use[***]"; }
    }

    record RecordFingerprint(String key) implements EncryptionKeyDecision {
        @Override public String toString() { return "RecordFingerprint[***]"; }
    }

    record Generate() implements EncryptionKeyDecision {}

    record Refuse(String reason) implements EncryptionKeyDecision {}

    static EncryptionKeyDecision decide(Optional<ProvidedKey> provided, InstanceState state, String keyFileLocation) {
        if (provided.isPresent()) {
            ProvidedKey key = provided.get();
            if (key.value().length() < MIN_LENGTH) {
                return new Refuse("The encryption key from " + key.origin()
                    + " must be at least 32 characters for sufficient entropy");
            }
            if (state.fingerprint().isEmpty()) {
                return new RecordFingerprint(key.value());
            }
            if (state.fingerprint().get().matches(key.value())) {
                return new Use(key.value());
            }
            return new Refuse("The encryption key from " + key.origin() + " is not the one this database was "
                + "encrypted with. Nothing was changed. Put back the key this installation used — in "
                + keyFileLocation + " or in NIDO_ENCRYPTION_SECRET — and start again.");
        }
        if (state.fingerprint().isEmpty() && !state.setupCompleted()) {
            return new Generate();
        }
        return new Refuse("This database holds data encrypted with a key that was not provided. Restore "
            + keyFileLocation + " from your backup, or set NIDO_ENCRYPTION_SECRET to the key this installation used.");
    }
}
