package com.boilerplate.api.identity.infrastructure.security;

import com.boilerplate.api.identity.domain.port.out.TotpRecordInitPort;
import com.boilerplate.api.mfa.application.port.in.InitTotpRecordUseCase;
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