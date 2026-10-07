package com.nido.api.instance.domain.model;

import java.util.Objects;
import java.util.Optional;

/**
 * The key this start resolved, and the fingerprint it recorded for it when the installation had none. That fingerprint
 * is only taken on the data's word: the key check gives it back to {@code ForgetEncryptionKeyFingerprintUseCase} when
 * the data already encrypted proves the key wrong, or the right key would be refused at the next start.
 */
public record ResolvedEncryptionKey(String value, Optional<KeyFingerprint> recordedAtThisStart) {

    public ResolvedEncryptionKey {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(recordedAtThisStart, "recordedAtThisStart");
    }

    /** The key itself never reaches a log through this. */
    @Override
    public String toString() {
        return "ResolvedEncryptionKey[***]";
    }
}
