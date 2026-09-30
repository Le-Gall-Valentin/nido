package com.nido.api.authentication.application.service;

import com.nido.api.authentication.domain.port.out.PasswordResetTokenRepository;
import com.nido.api.authentication.domain.port.out.RefreshTokenRevocationPort;
import com.nido.api.authentication.domain.port.out.UserCredentialPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeleteUserDataServiceTest {

    @Mock UserCredentialPort credentials;
    @Mock RefreshTokenRevocationPort refreshTokens;
    @Mock PasswordResetTokenRepository resetTokens;

    @Test
    void a_deleted_account_leaves_no_reset_link_behind() {
        // Accounts are anonymised by an UPDATE, which the foreign key's ON DELETE CASCADE never sees.
        UUID userId = UUID.randomUUID();

        new DeleteUserDataService(credentials, refreshTokens, resetTokens).delete(userId);

        verify(resetTokens).deleteAllForUser(userId);
        verify(refreshTokens).deleteAllForUser(userId);
        verify(credentials).deleteCredential(userId);
    }
}
