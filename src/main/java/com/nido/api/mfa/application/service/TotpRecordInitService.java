package com.nido.api.mfa.application.service;

import com.nido.api.mfa.application.port.in.InitTotpRecordUseCase;
import com.nido.api.mfa.domain.port.out.UserTotpInitPort;
import com.nido.api.shared.annotation.ApplicationService;
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