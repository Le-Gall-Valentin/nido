package com.nido.api.identity.infrastructure.security;

import com.nido.api.mfa.application.port.in.InitTotpRecordUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TotpRecordInitAdapterTest {

    @Mock InitTotpRecordUseCase initTotpRecordUseCase;

    @Test
    void initForUser_delegatesToInitTotpRecordUseCase() {
        TotpRecordInitAdapter adapter = new TotpRecordInitAdapter(initTotpRecordUseCase);
        UUID userId = UUID.randomUUID();

        adapter.initForUser(userId);

        verify(initTotpRecordUseCase).initForNewUser(userId);
    }
}
