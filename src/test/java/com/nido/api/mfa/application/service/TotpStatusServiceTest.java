package com.nido.api.mfa.application.service;

import com.nido.api.mfa.domain.port.out.UserTotpQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TotpStatusServiceTest {

    @Mock UserTotpQueryPort userTotpQuery;

    private TotpStatusService service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new TotpStatusService(userTotpQuery);
    }

    @Test
    void the_accounts_with_the_app_on_are_read_from_the_store() {
        when(userTotpQuery.findTotpEnabledAmong(List.of(userId))).thenReturn(Set.of(userId));

        assertThat(service.findTotpEnabledAmong(List.of(userId))).containsExactly(userId);
    }
}
