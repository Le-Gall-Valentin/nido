package com.nido.api.instance.domain.model;

import java.util.Optional;

/**
 * The installation as the database knows it. {@code keyGenerated}: Nido made the key itself, so the
 * setup screen still has to show it once. {@code fingerprintConfirmed}: the data already encrypted opened
 * with the key the fingerprint recognises — until then, the fingerprint was only taken on the data's word.
 */
public record InstanceState(Optional<KeyFingerprint> fingerprint, boolean fingerprintConfirmed, boolean keyGenerated,
                            boolean setupCompleted) {

    /** The fingerprint the data has yet to confirm, if that is the one in place. */
    public Optional<KeyFingerprint> unconfirmedFingerprint() {
        return fingerprintConfirmed ? Optional.empty() : fingerprint;
    }
}
