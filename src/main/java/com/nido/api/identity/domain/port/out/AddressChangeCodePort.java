package com.nido.api.identity.domain.port.out;

import com.nido.api.identity.domain.model.EmailCodeCheck;
import com.nido.api.identity.domain.model.EmailCodeDelivery;

import java.util.UUID;

/**
 * The code that proves a new address receives mail, asked before an account whose second factor is the mail
 * moves to it. mfa's, through this port. Addresses are given normalised.
 */
public interface AddressChangeCodePort {

    /** Whether the code by mail protects the account, paused or not: then a new address is proven, or it goes. */
    boolean mailMethodOn(UUID userId);

    EmailCodeDelivery send(UUID userId, String newAddress);

    /** A right code is used up; five wrong ones take it with them. */
    EmailCodeCheck check(UUID userId, String newAddress, String code);

    /** The new address could not be proven: the code by mail is turned off, and its holder told. */
    void forgoMailMethod(UUID userId);
}
