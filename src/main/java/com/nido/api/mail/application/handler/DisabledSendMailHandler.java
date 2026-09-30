package com.nido.api.mail.application.handler;

import com.nido.api.mail.application.port.in.SendMailUseCase;
import com.nido.api.mail.domain.model.MailRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code send} when mail is off: accepted, and dropped. Callers never ask first — a password change
 * that would have sent an alert simply sends none — and nothing that could deliver a mail exists.
 */
public class DisabledSendMailHandler implements SendMailUseCase {

    private static final Logger log = LoggerFactory.getLogger(DisabledSendMailHandler.class);

    @Override
    public void send(MailRequest request) {
        log.debug("Mail is off: {} not sent", request.content().template());
    }
}
