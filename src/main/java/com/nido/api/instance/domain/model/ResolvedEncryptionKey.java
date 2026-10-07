package com.nido.api.instance.domain.model;

import java.util.Objects;
import java.util.Optional;

/**
 * The key this start resolved, and its fingerprint while the data already encrypted has yet to confirm it: recorded
 * for an installation that had none, at this start or at one that stopped before its key check. The key check settles
 * it — {@code ConfirmEncryptionKeyFingerprintUseCase} when the data opens with the key,
 * {@code ForgetEncryptionKeyFingerprintUseCase} when it does not, or the right key would be refused at the next start.
 */
public record ResolvedEncryptionKey(String value, Optional<KeyFingerprint> awaitingConfirmation) {

    public ResolvedEncryptionKey {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(awaitingConfirmation, "awaitingConfirmation");
    }

    /** The key itself never reaches a log through this. */
    @Override
    public String toString() {
        return "ResolvedEncryptionKey[***]";
    }
}
