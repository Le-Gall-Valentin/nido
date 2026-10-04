package com.nido.api.mail.application.port.in;

/**
 * Withdraws the mails still waiting for an address: an account being erased must not be written to
 * afterwards. Joins the caller's transaction. A mail already handed to the SMTP server is beyond reach.
 */
public interface CancelPendingMailsUseCase {

    /** @return how many mails were withdrawn */
    int cancelPendingMailsTo(String address);
}
