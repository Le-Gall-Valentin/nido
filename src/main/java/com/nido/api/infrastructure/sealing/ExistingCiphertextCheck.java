package com.nido.api.infrastructure.sealing;

/**
 * Proof that the key this start was given is the one the data already encrypted was encrypted with,
 * asked before the migration seals anything with it. The fingerprint of 0.12 already refuses another
 * key; this covers an installation older than that, upgraded straight to a version that encrypts more:
 * with the wrong key, its shopping lists would end up under a key its finance never had.
 */
public interface ExistingCiphertextCheck {

    /** @throws IllegalStateException naming what does not decrypt — a space, or the two-factor secrets */
    void verify();
}
