package com.nido.api.identity.infrastructure.security;

import com.nido.api.mfa.application.port.in.AdminDisableTotpUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void hands_back_whether_a_second_factor_was_on_which_decides_who_is_told() {
        MfaAdminResetTotpAdapter adapter = new MfaAdminResetTotpAdapter(adminDisableTotpUseCase);
        UUID on = UUID.randomUUID();
        UUID off = UUID.randomUUID();
        when(adminDisableTotpUseCase.disableIfEnabled(on)).thenReturn(true);
        when(adminDisableTotpUseCase.disableIfEnabled(off)).thenReturn(false);

        assertThat(adapter.disableTotpIfEnabled(on)).isTrue();
        assertThat(adapter.disableTotpIfEnabled(off)).isFalse();
    }
}
