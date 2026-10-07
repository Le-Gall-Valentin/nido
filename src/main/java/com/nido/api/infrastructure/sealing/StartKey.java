package com.nido.api.infrastructure.sealing;

/**
 * What the key check needs to know of the key this start was given: whether the data already encrypted has yet to
 * confirm it — its fingerprint was recorded for an installation that had none (older than 0.12), at this start or at
 * one that stopped before its key check — and how to pass on the check's verdict. The instance module, which resolves
 * the key, provides it.
 */
public interface StartKey {

    boolean awaitsConfirmation();

    /** The data opened with the key: its fingerprint stays for good. Does nothing for a key confirmed already. */
    void confirm();

    /**
     * The data refused the key: its fingerprint goes, or the right key would be refused at the next start. Does
     * nothing for a key confirmed already.
     */
    void forget();
}
