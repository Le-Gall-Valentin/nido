package com.nido.api.mfa.domain.port.out;

import com.nido.api.mfa.domain.model.CodePurpose;

import java.util.UUID;

/**
 * Wrong codes given to the authenticator app outside sign-in, counted apart for each purpose: proving a new
 * one ({@link CodePurpose#ENROL}) and turning it off ({@link CodePurpose#DISABLE}). Sign-in is counted by the
 * account, in authentication.
 */
public interface TotpAttemptPort {

    /** @return the failures now recorded for this purpose */
    int recordFailure(UUID userId, CodePurpose purpose);

    int failures(UUID userId, CodePurpose purpose);

    void clear(UUID userId, CodePurpose purpose);
}
