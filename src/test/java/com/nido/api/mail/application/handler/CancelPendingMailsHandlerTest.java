package com.nido.api.mail.application.handler;

import com.nido.api.mail.domain.port.out.MailOutboxPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelPendingMailsHandlerTest {

    @Mock MailOutboxPort outbox;

    @Test
    void the_mails_waiting_for_the_address_are_withdrawn() {
        when(outbox.deleteAddressedTo("jane@test.local")).thenReturn(2);

        assertThat(new CancelPendingMailsHandler(outbox).cancelPendingMailsTo("jane@test.local")).isEqualTo(2);
    }
}
