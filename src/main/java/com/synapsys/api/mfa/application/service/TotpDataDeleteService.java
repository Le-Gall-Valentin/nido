package com.synapsys.api.mfa.application.service;

import com.synapsys.api.mfa.application.port.in.DeleteTotpDataUseCase;
import com.synapsys.api.mfa.domain.port.out.UserTotpLifecyclePort;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@ApplicationService
public class TotpDataDeleteService implements DeleteTotpDataUseCase {

    private final UserTotpLifecyclePort userTotpLifecyclePort;

    public TotpDataDeleteService(UserTotpLifecyclePort userTotpLifecyclePort) {
        this.userTotpLifecyclePort = userTotpLifecyclePort;
    }

    @Override
    @Transactional
    public void deleteUserData(UUID userId) {
        userTotpLifecyclePort.deleteTotp(userId);
    }
}