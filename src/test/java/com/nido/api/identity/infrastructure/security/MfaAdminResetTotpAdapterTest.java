package com.nido.api.identity.infrastructure.security;

import com.nido.api.mfa.application.port.in.AdminDisableTotpUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MfaAdminResetTotpAdapterTest {

    @Mock AdminDisableTotpUseCase adminDisableTotpUseCase;

    @Test
    void disableTotpIfEnabled_delegatesToAdminDisableTotpUseCase() {
        MfaAdminResetTotpAdapter adapter = new MfaAdminResetTotpAdapter(adminDisableTotpUseCase);
        UUID userId = UUID.randomUUID();

        adapter.disableTotpIfEnabled(userId);

        verify(adminDisableTotpUseCase).disableIfEnabled(userId);
    }
}
