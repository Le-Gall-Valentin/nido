package com.nido.api.authentication.domain.port.out;

import com.nido.api.authentication.domain.model.AccountContact;

import java.time.Duration;
import java.time.Instant;

/** The mails authentication sends about an account. Accepted and dropped when mail is off. */
public interface AccountMailPort {

    /** Whether a mail can actually leave — what decides if "forgot password" is offered at all. */
    boolean canSend();

    void passwordResetRequested(AccountContact account, String rawToken, Instant expiresAt, Duration validity);

    void passwordChanged(AccountContact account);
}
