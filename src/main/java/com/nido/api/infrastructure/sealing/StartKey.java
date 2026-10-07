package com.nido.api.infrastructure.sealing;

/**
 * What the key check needs to know of the key this start was given: whether this start recorded its fingerprint —
 * an installation older than 0.12 had none — and how to take that back when the data already encrypted refuses the
 * key, or the right key would be refused at the next start. The instance module, which resolves the key, provides it.
 */
public interface StartKey {

    boolean fingerprintRecordedAtThisStart();

    /** Does nothing when this start recorded no fingerprint. */
    void forgetFingerprintRecordedAtThisStart();
}
