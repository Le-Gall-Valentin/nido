package com.nido.api.mail.domain.port.out;

import com.nido.api.mail.domain.model.DeliveryOutcome;
import com.nido.api.mail.domain.model.OutgoingMail;

public interface MailTransportPort {
    /** Tries once. Never throws for a delivery problem: the outcome says what happened. */
    DeliveryOutcome deliver(OutgoingMail mail);
}
