package com.nido.api.identity.infrastructure.mail;

import com.nido.api.identity.domain.port.out.PendingMailCancellationPort;
import com.nido.api.mail.application.port.in.CancelPendingMailsUseCase;
import org.springframework.stereotype.Component;

@Component
public class PendingMailCancellationAdapter implements PendingMailCancellationPort {

    private final CancelPendingMailsUseCase cancelPendingMails;

    public PendingMailCancellationAdapter(CancelPendingMailsUseCase cancelPendingMails) {
        this.cancelPendingMails = cancelPendingMails;
    }

    @Override
    public void cancelPendingMailsTo(String address) {
        cancelPendingMails.cancelPendingMailsTo(address);
    }
}
