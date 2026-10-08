package com.nido.api.mfa.domain.port.out;

import com.nido.api.mfa.domain.model.CodePurpose;

import java.time.Instant;
import java.util.UUID;

/** mfa's mails. The address is given: a code to confirm a new address goes to that address. */
public interface TwoFactorMailPort {

    /** Queues the code in the account's language; the mail dies with the code. */
    void sendCode(UUID userId, String address, CodePurpose purpose, String code, Instant expiresAt);
}
