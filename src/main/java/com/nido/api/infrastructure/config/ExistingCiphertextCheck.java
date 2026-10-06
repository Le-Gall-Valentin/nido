package com.nido.api.infrastructure.config;

/**
 * Proof that the key this start was given is the one the data already encrypted was encrypted with,
 * asked before a backfill encrypts anything with it. The fingerprint of 0.12 already refuses another
 * key; this covers an installation older than that, upgraded straight to a version that encrypts more:
 * with the wrong key, its shopping lists would end up under a key its finance never had.
 */
public interface ExistingCiphertextCheck {

    /** @throws IllegalStateException naming the space whose data does not decrypt */
    void verify();
}
