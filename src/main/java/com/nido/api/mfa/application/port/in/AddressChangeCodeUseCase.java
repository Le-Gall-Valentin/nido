package com.nido.api.mfa.application.port.in;

import com.nido.api.mfa.domain.model.CodeCheck;
import com.nido.api.mfa.domain.model.CodeDelivery;

import java.util.UUID;

/**
 * The code that proves a new address receives mail, asked before an account whose second factor is the mail
 * moves to it — a typo would otherwise lock its holder out at the next sign-in. The address comes normalised.
 * Nothing here throws for a refusal: the caller words it.
 */
public interface AddressChangeCodeUseCase {

    /** Whether the mail method is on, paused or not: a new address is then proven, or the method goes. */
    boolean mailMethodOn(UUID userId);

    /** {@link CodeDelivery.Unavailable} while mail is off: nothing can prove the address then. */
    CodeDelivery send(UUID userId, String newAddress);

    /** A right code is used up; five wrong ones take it with them. */
    CodeCheck check(UUID userId, String newAddress, String code);

    /**
     * The account moved to an address nothing proved: the mail method, which would send every code there, is
     * turned off and its holder told.
     */
    void forgoMailMethod(UUID userId);
}
