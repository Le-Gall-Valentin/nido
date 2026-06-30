package com.synapsys.api.mfa.application.service;

import com.synapsys.api.mfa.domain.model.UserTotpProfile;
import com.synapsys.api.mfa.domain.port.out.UserTotpQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
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
    void isTotpEnabled_userFoundAndTotpEnabled_returnsTrue() {
        UserTotpProfile profile = new UserTotpProfile(userId,true, Optional.of("SECRET"));
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));

        assertThat(service.isTotpEnabled(userId)).isTrue();
    }

    @Test
    void isTotpEnabled_userFoundAndTotpDisabled_returnsFalse() {
        UserTotpProfile profile = new UserTotpProfile(userId,false, Optional.empty());
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));

        assertThat(service.isTotpEnabled(userId)).isFalse();
    }

    @Test
    void isTotpEnabled_userNotFound_returnsFalse() {
        when(userTotpQuery.findById(userId)).thenReturn(Optional.empty());

        assertThat(service.isTotpEnabled(userId)).isFalse();
    }
}