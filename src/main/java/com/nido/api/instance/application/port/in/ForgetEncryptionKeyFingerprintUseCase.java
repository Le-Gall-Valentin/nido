package com.nido.api.instance.application.port.in;

import com.nido.api.instance.domain.model.KeyFingerprint;

/** Takes back a fingerprint the data has yet to confirm, once the data already encrypted proves its key wrong. */
public interface ForgetEncryptionKeyFingerprintUseCase {

    /** Erases this fingerprint if it is still the one in place and nothing confirmed it; does nothing otherwise. */
    void forget(KeyFingerprint fingerprint);
}
