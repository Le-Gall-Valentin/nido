package com.nido.api.mfa.domain.model;

/** What a code sent by mail is for. An account has at most one live code per purpose. */
public enum CodePurpose {
    /** The second step of a sign-in. */
    LOGIN,
    /** Proving the account's address receives mail before turning the mail method on. */
    ENROL,
    /** Proving it again before turning the mail method off. */
    DISABLE,
    /** Proving a new address receives mail before the account moves to it. */
    EMAIL_CHANGE
}
