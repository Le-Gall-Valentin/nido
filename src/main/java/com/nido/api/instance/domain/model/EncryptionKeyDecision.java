package com.nido.api.instance.domain.model;

import java.util.Optional;

/**
 * What a start does about the encryption key, from the key it was given and what the database
 * remembers. Until the setup is done, nothing is encrypted: an installation is marked set up as soon
 * as it has accounts (migration 064, the seed, the setup screen — each in the transaction that creates
 * the first one), so "not set up" is "no account" without asking the identity context. A fingerprint
 * recorded before that protects nothing yet: a generated key lost with its volume before the end of
 * the setup — the one moment it is shown — is simply generated again, instead of a refusal to restore
 * a key nobody has seen.
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
            if (!state.setupCompleted()) {
                return new RecordFingerprint(key.value());
            }
            return new Refuse("The encryption key from " + key.origin() + " is not the one this database was "
                + "encrypted with. Nothing was changed. Put back the key this installation used — in "
                + keyFileLocation + " or in NIDO_ENCRYPTION_SECRET — and start again.");
        }
        if (!state.setupCompleted()) {
            return new Generate();
        }
        return new Refuse("This database holds data encrypted with a key that was not provided. Restore "
            + keyFileLocation + " from your backup, or set NIDO_ENCRYPTION_SECRET to the key this installation used.");
    }
}
