package com.boilerplate.api.mfa.application.service;

import com.boilerplate.api.mfa.application.port.in.InitTotpRecordUseCase;
import com.boilerplate.api.mfa.domain.port.out.UserTotpInitPort;
import com.boilerplate.api.shared.annotation.ApplicationService;
import java.util.UUID;

@ApplicationService
public class TotpRecordInitService implements InitTotpRecordUseCase {

    private final UserTotpInitPort userTotpInitPort;

    public TotpRecordInitService(UserTotpInitPort userTotpInitPort) {
        this.userTotpInitPort = userTotpInitPort;
    }

    @Override
    public void initForNewUser(UUID userId) {
        userTotpInitPort.createDefaultRecord(userId);
    }
}