package com.nido.api.mfa.application.service;

import com.nido.api.mfa.domain.port.out.UserTotpLifecyclePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TotpDataDeleteServiceTest {

    @Mock UserTotpLifecyclePort userTotpLifecyclePort;

    private TotpDataDeleteService service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new TotpDataDeleteService(userTotpLifecyclePort);
    }

    @Test
    void deleteUserData_delegatesToTheLifecyclePort() {
        service.deleteUserData(userId);

        verify(userTotpLifecyclePort).deleteTotp(userId);
    }
}
