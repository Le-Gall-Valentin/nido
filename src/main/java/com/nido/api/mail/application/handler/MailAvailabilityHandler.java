package com.nido.api.mail.application.handler;

import com.nido.api.mail.application.port.in.MailAvailabilityQuery;
import com.nido.api.mail.domain.port.out.MailConfigurationPort;

/** Whether a mail could leave right now. */
public class MailAvailabilityHandler implements MailAvailabilityQuery {

    private final MailConfigurationPort configuration;

    public MailAvailabilityHandler(MailConfigurationPort configuration) {
        this.configuration = configuration;
    }

    @Override
    public boolean isAvailable() {
        return configuration.active().isPresent();
    }
}
