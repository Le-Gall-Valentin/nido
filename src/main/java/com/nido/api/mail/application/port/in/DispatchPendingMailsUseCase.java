package com.nido.api.mail.application.port.in;

/** Sends every mail that is due now. Internal to the mail context: its trigger and sweep call it. */
public interface DispatchPendingMailsUseCase {
    /** @return how many outbox entries were handled (sent, rescheduled or dropped) */
    int dispatch();
}
