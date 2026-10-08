package com.nido.api.mfa.domain.model;

/** What a code was found to be. REPLAYED: right, but already used — not counted as a failure. */
public enum CodeCheck {
    SUCCESS,
    INVALID,
    REPLAYED
}
