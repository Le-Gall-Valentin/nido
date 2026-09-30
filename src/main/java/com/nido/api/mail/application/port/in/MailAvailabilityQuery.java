package com.nido.api.mail.application.port.in;

/** Whether mail is switched on — for a feature that only makes sense when a mail can actually leave. */
public interface MailAvailabilityQuery {
    boolean isAvailable();
}
