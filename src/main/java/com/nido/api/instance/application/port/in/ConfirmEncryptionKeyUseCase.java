package com.nido.api.instance.application.port.in;

/**
 * Whether the key this start resolved is new to the installation — its fingerprint recorded by this very
 * start, for an installation that had none — and how to take that back when the data already encrypted
 * proves it is the wrong one. Without taking it back, the right key would be refused at the next start.
 */
public interface ConfirmEncryptionKeyUseCase {

    /** True when this start recorded the fingerprint of the key it was given. */
    boolean recordedAtThisStart();

    /** Erases the fingerprint this start recorded, if it is still the one in place; does nothing otherwise. */
    void forgetFingerprintRecordedAtThisStart();
}
