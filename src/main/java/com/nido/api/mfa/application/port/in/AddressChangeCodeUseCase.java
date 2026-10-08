package com.nido.api.mfa.application.port.in;

import java.util.UUID;

/**
 * The code that proves a new address receives mail, asked before an account whose second factor is the mail
 * moves to it — a typo would otherwise lock its holder out at the next sign-in. The address comes normalised.
 */
public interface AddressChangeCodeUseCase {

    /** Whether the mail method is on and usable: only then is the new address proven first. */
    boolean required(UUID userId);

    /** @return the seconds before another code can be asked for */
    long send(UUID userId, String newAddress);

    /** A right code is used up: true once. */
    boolean check(UUID userId, String newAddress, String code);
}
