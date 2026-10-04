package com.nido.api.identity.infrastructure.mail;

import com.nido.api.mail.application.port.in.CancelPendingMailsUseCase;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PendingMailCancellationAdapterTest {

    @Test
    void it_hands_the_address_to_the_mail_context() {
        CancelPendingMailsUseCase cancel = mock(CancelPendingMailsUseCase.class);

        new PendingMailCancellationAdapter(cancel).cancelPendingMailsTo("jane@test.local");

        verify(cancel).cancelPendingMailsTo("jane@test.local");
    }
}
