package com.nido.api.mail.application.handler;

import com.nido.api.mail.application.port.in.CancelPendingMailsUseCase;
import com.nido.api.mail.domain.port.out.MailOutboxPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/** Withdraws, when mail is on, the queued mails of an address whose account is being erased. */
public class CancelPendingMailsHandler implements CancelPendingMailsUseCase {

    private static final Logger log = LoggerFactory.getLogger(CancelPendingMailsHandler.class);

    private final MailOutboxPort outbox;

    public CancelPendingMailsHandler(MailOutboxPort outbox) {
        this.outbox = outbox;
    }

    @Override
    @Transactional
    public int cancelPendingMailsTo(String address) {
        int cancelled = outbox.deleteAddressedTo(address);
        if (cancelled > 0) {
            log.info("{} queued mail(s) withdrawn: their recipient's account is being erased", cancelled);
        }
        return cancelled;
    }
}
