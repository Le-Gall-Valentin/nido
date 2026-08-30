package com.nido.api.identity.infrastructure.security;

import com.nido.api.mfa.application.port.in.DeleteTotpDataUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TotpDeletionAdapterTest {

    @Mock DeleteTotpDataUseCase deleteTotpDataUseCase;

    @Test
    void deleteTotpData_delegatesToDeleteTotpDataUseCase() {
        TotpDeletionAdapter adapter = new TotpDeletionAdapter(deleteTotpDataUseCase);
        UUID userId = UUID.randomUUID();

        adapter.deleteTotpData(userId);

        verify(deleteTotpDataUseCase).deleteUserData(userId);
    }
}
