package com.nido.api.mfa.domain.model;

/** What a code was found to be. Only a wrong code is counted: the others say why nothing could be checked. */
public enum CodeCheck {
    SUCCESS,
    /** Wrong: the code waiting still stands. */
    INVALID,
    /** Right, but already used — not counted as a failure. */
    REPLAYED,
    /** Nothing was waiting for it — never sent, used, replaced or expired: a new one has to be asked for. */
    EXPIRED,
    /**
     * Wrong once too often: a code sent is gone with it, and the app's codes are refused for a while. Never at
     * sign-in, where the account's own counter is the one that locks.
     */
    SPENT
}
