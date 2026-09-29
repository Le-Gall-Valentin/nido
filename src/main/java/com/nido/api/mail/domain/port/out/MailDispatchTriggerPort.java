package com.nido.api.mail.domain.port.out;

public interface MailDispatchTriggerPort {
    /**
     * Asks for a dispatch once the current transaction commits — and never if it rolls back. With no
     * transaction in progress, asks for one right away.
     */
    void wakeUpAfterCommit();
}
