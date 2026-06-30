package com.synapsys.api.mfa.application.service;

import com.synapsys.api.mfa.domain.model.UserTotpProfile;
import com.synapsys.api.mfa.domain.port.out.UserTotpLifecyclePort;
import com.synapsys.api.mfa.domain.port.out.UserTotpQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminTotpDisableServiceTest {

    @Mock UserTotpQueryPort userTotpQuery;
    @Mock UserTotpLifecyclePort userTotpLifecyclePort;

    private AdminTotpDisableService service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AdminTotpDisableService(userTotpQuery, userTotpLifecyclePort);
    }

    @Test
    void disableIfEnabled_totpEnabled_callsDisableTotp() {
        UserTotpProfile profile = new UserTotpProfile(userId,true, Optional.of("SECRET"));
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));

        service.disableIfEnabled(userId);

        verify(userTotpLifecyclePort).disableTotp(userId);
    }

    @Test
    void disableIfEnabled_totpDisabled_noOp() {
        UserTotpProfile profile = new UserTotpProfile(userId,false, Optional.empty());
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));

        service.disableIfEnabled(userId);

        verify(userTotpLifecyclePort, never()).disableTotp(any());
    }

    @Test
    void disableIfEnabled_userAbsent_noOp() {
        when(userTotpQuery.findById(userId)).thenReturn(Optional.empty());

        service.disableIfEnabled(userId);

        verify(userTotpLifecyclePort, never()).disableTotp(any());
    }
}