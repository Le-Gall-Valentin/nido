package com.nido.api.instance.application.port.in;

import com.nido.api.instance.domain.model.KeyFingerprint;

/** Takes back the fingerprint a start recorded, once the data already encrypted proves its key is the wrong one. */
public interface ForgetEncryptionKeyFingerprintUseCase {

    /** Erases this fingerprint if it is still the one in place; does nothing otherwise. */
    void forget(KeyFingerprint fingerprint);
}
