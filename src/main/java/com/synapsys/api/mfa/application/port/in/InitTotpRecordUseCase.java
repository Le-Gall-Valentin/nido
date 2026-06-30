package com.synapsys.api.mfa.application.port.in;

import java.util.UUID;

public interface InitTotpRecordUseCase {
    void initForNewUser(UUID userId);
}