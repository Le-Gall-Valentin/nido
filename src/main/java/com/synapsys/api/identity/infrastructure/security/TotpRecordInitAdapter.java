package com.synapsys.api.identity.infrastructure.security;

import com.synapsys.api.identity.domain.port.out.TotpRecordInitPort;
import com.synapsys.api.mfa.application.port.in.InitTotpRecordUseCase;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class TotpRecordInitAdapter implements TotpRecordInitPort {

    private final InitTotpRecordUseCase initTotpRecordUseCase;

    public TotpRecordInitAdapter(InitTotpRecordUseCase initTotpRecordUseCase) {
        this.initTotpRecordUseCase = initTotpRecordUseCase;
    }

    @Override
    public void initForUser(UUID userId) {
        initTotpRecordUseCase.initForNewUser(userId);
    }
}