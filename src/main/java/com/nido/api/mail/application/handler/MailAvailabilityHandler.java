package com.nido.api.mail.application.handler;

import com.nido.api.mail.application.port.in.MailAvailabilityQuery;

/** Answers with the mode the configuration chose; one instance per mode. */
public class MailAvailabilityHandler implements MailAvailabilityQuery {

    private final boolean available;

    public MailAvailabilityHandler(boolean available) {
        this.available = available;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }
}
