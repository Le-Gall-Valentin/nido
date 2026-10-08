package com.nido.api.mfa.domain.port.out;

import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.mfa.domain.model.SentMailCode;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** The live code of each account and purpose. */
public interface MailCodeStorePort {

    /** Expired or not: the caller decides. */
    Optional<SentMailCode> find(UUID userId, CodePurpose purpose);

    /** Writes this code over any other for the same account and purpose, its failures back at zero. */
    void replace(SentMailCode code);

    /** @return the failures now recorded, 0 when there is no code */
    int recordFailure(UUID userId, CodePurpose purpose);

    /**
     * Deletes the code if it is still the one with this hash and fewer than five failures are recorded against it:
     * of two requests carrying the right code at once, one takes it and the other finds nothing.
     *
     * @return whether this caller took it
     */
    boolean take(UUID userId, CodePurpose purpose, String codeHash);

    void delete(UUID userId, CodePurpose purpose);

    void deleteAll(UUID userId);

    /** @return how many expired codes went */
    int deleteExpired(Instant now);
}
