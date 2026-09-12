package com.nido.api.mfa.application.service;

import com.nido.api.mfa.application.port.in.DeleteTotpDataUseCase;
import com.nido.api.mfa.domain.port.out.PendingTotpEnrolmentPort;
import com.nido.api.mfa.domain.port.out.UserTotpLifecyclePort;
import com.nido.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@ApplicationService
public class TotpDataDeleteService implements DeleteTotpDataUseCase {

    private final UserTotpLifecyclePort userTotpLifecyclePort;
    private final PendingTotpEnrolmentPort pendingEnrolment;

    public TotpDataDeleteService(UserTotpLifecyclePort userTotpLifecyclePort,
                                 PendingTotpEnrolmentPort pendingEnrolment) {
        this.userTotpLifecyclePort = userTotpLifecyclePort;
        this.pendingEnrolment = pendingEnrolment;
    }

    @Override
    @Transactional
    public void deleteUserData(UUID userId) {
        // Erasing a user's data means all of it, including an enrolment they had in
        // flight. It would expire on its own, but "eventually" is not what deletion means.
        pendingEnrolment.discard(userId);
        userTotpLifecyclePort.deleteTotp(userId);
    }
}