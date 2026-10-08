package com.nido.api.mfa.domain.port.out;

import java.util.OptionalLong;
import java.util.UUID;

/**
 * How many mails of code an account may be sent: a sign-in with the right password sends one, so whoever
 * knows the password must not be able to fill the holder's mailbox.
 */
public interface MailCodeSendLimitPort {

    /**
     * Counts one mail against the account: empty when it may leave, else the seconds until it may. A mail whose
     * transaction rolls back never leaves, and is given back.
     */
    OptionalLong tryCount(UUID userId);
}
