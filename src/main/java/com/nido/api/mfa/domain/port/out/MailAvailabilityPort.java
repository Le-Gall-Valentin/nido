package com.nido.api.mfa.domain.port.out;

/** Whether a mail could leave right now: mail can be switched on and off without a restart. */
public interface MailAvailabilityPort {
    boolean isAvailable();
}
