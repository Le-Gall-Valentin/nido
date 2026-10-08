package com.nido.api.mfa.infrastructure.mail;

import com.nido.api.mail.application.port.in.MailAvailabilityQuery;
import com.nido.api.mfa.domain.port.out.MailAvailabilityPort;
import org.springframework.stereotype.Component;

@Component
public class MailAvailabilityAdapter implements MailAvailabilityPort {

    private final MailAvailabilityQuery availability;

    public MailAvailabilityAdapter(MailAvailabilityQuery availability) {
        this.availability = availability;
    }

    @Override
    public boolean isAvailable() {
        return availability.isAvailable();
    }
}
