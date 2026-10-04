package com.nido.api.identity.domain.port.out;

/** Withdraws the mails still waiting to be sent to an address: an erased account is written to no more. */
public interface PendingMailCancellationPort {
    void cancelPendingMailsTo(String address);
}
