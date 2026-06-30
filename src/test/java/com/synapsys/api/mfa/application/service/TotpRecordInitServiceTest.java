package com.synapsys.api.mfa.application.service;

import com.synapsys.api.mfa.domain.port.out.UserTotpInitPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TotpRecordInitServiceTest {

    @Mock UserTotpInitPort userTotpInitPort;
    private TotpRecordInitService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() { service = new TotpRecordInitService(userTotpInitPort); }

    @Test
    void initForNewUser_delegatesCreateDefaultRecord() {
        UUID userId = UUID.randomUUID();

        service.initForNewUser(userId);

        verify(userTotpInitPort).createDefaultRecord(userId);
    }
}