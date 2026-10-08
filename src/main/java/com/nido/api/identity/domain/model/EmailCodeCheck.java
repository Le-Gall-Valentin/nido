package com.nido.api.identity.domain.model;

/** What the code given for a new address was found to be. */
public enum EmailCodeCheck {
    VALID,
    /** Wrong: the code sent still stands. */
    INVALID,
    /** None is waiting for this address — never sent, or expired: a new one has to be asked for. */
    EXPIRED,
    /** Wrong once too often: the code sent is gone with it. */
    SPENT
}
