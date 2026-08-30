package com.nido.api.identity.infrastructure.security;

import com.nido.api.mfa.application.port.in.GetTotpStatusUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityTotpStatusAdapterTest {

    @Mock GetTotpStatusUseCase getTotpStatusUseCase;

    @Test
    void isTotpEnabled_delegatesToGetTotpStatusUseCase() {
        IdentityTotpStatusAdapter adapter = new IdentityTotpStatusAdapter(getTotpStatusUseCase);
        UUID userId = UUID.randomUUID();
        when(getTotpStatusUseCase.isTotpEnabled(userId)).thenReturn(true);

        assertThat(adapter.isTotpEnabled(userId)).isTrue();
    }

    @Test
    void findTotpEnabledAmong_delegatesToGetTotpStatusUseCase() {
        IdentityTotpStatusAdapter adapter = new IdentityTotpStatusAdapter(getTotpStatusUseCase);
        UUID enabledId = UUID.randomUUID();
        UUID disabledId = UUID.randomUUID();
        when(getTotpStatusUseCase.findTotpEnabledAmong(List.of(enabledId, disabledId))).thenReturn(Set.of(enabledId));

        assertThat(adapter.findTotpEnabledAmong(List.of(enabledId, disabledId))).containsExactly(enabledId);
    }
}
