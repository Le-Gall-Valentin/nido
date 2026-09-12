package com.nido.api.mfa.domain.port.out;

import java.util.UUID;

public interface UserTotpLifecyclePort {
    /**
     * Promotes a proven enrolment to the account's active authenticator. Takes the secret because
     * this is the write that makes it durable — until now it only existed for the length of the
     * enrolment.
     */
    void enableTotp(UUID userId, String secret);
    void disableTotp(UUID userId);
    void deleteTotp(UUID userId);
}