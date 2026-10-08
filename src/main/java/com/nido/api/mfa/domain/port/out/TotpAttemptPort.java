package com.nido.api.mfa.domain.port.out;

import com.nido.api.mfa.domain.model.CodePurpose;

import java.util.UUID;

/**
 * Codes given to the authenticator app outside sign-in, counted apart for each purpose: proving a new one
 * ({@link CodePurpose#ENROL}) and turning it off ({@link CodePurpose#DISABLE}). Sign-in is counted by the
 * account, in authentication.
 */
public interface TotpAttemptPort {

    /**
     * Counts one more, atomically, in a window the first one opens and the later ones leave where it is.
     *
     * @return the count now, this one included
     */
    int record(UUID userId, CodePurpose purpose);

    void clear(UUID userId, CodePurpose purpose);
}
