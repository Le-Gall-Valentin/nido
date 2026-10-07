package com.nido.api.instance.application.port.in;

import com.nido.api.instance.domain.model.KeyFingerprint;

/** Keeps a fingerprint for good, once the data already encrypted opened with its key. */
public interface ConfirmEncryptionKeyFingerprintUseCase {

    /** Confirms this fingerprint if it is still the one in place; does nothing otherwise. */
    void confirm(KeyFingerprint fingerprint);
}
