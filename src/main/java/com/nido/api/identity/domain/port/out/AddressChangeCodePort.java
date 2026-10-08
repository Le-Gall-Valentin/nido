package com.nido.api.identity.domain.port.out;

import com.nido.api.identity.domain.model.EmailCodeCheck;

import java.util.UUID;

/**
 * The code that proves a new address receives mail, asked before an account whose second factor is the mail
 * moves to it. mfa's, through this port. Addresses are given normalised.
 */
public interface AddressChangeCodePort {

    boolean required(UUID userId);

    /** @return the seconds before another code can be asked for */
    long send(UUID userId, String newAddress);

    /** A right code is used up; five wrong ones take it with them. */
    EmailCodeCheck check(UUID userId, String newAddress, String code);
}
