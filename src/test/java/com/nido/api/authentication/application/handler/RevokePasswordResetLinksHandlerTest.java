package com.nido.api.authentication.application.handler;

import com.nido.api.authentication.domain.port.out.PasswordResetTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RevokePasswordResetLinksHandlerTest {

    @Mock PasswordResetTokenRepository tokens;

    @Test
    void every_link_the_account_was_sent_goes() {
        UUID userId = UUID.randomUUID();

        new RevokePasswordResetLinksHandler(tokens).revokeFor(userId);

        verify(tokens).deleteAllForUser(userId);
    }
}
