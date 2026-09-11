package com.nido.api.mfa.application.service;

import com.nido.api.mfa.application.port.in.AdminDisableTotpUseCase;
import com.nido.api.mfa.domain.port.out.PendingTotpEnrolmentPort;
import com.nido.api.mfa.domain.port.out.UserTotpLifecyclePort;
import com.nido.api.mfa.domain.port.out.UserTotpQueryPort;
import com.nido.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@ApplicationService
public class AdminTotpDisableService implements AdminDisableTotpUseCase {

    private final UserTotpQueryPort userTotpQuery;
    private final UserTotpLifecyclePort userTotpLifecyclePort;
    private final PendingTotpEnrolmentPort pendingEnrolment;

    public AdminTotpDisableService(UserTotpQueryPort userTotpQuery, UserTotpLifecyclePort userTotpLifecyclePort,
                                   PendingTotpEnrolmentPort pendingEnrolment) {
        this.userTotpQuery = userTotpQuery;
        this.userTotpLifecyclePort = userTotpLifecyclePort;
        this.pendingEnrolment = pendingEnrolment;
    }

    @Transactional
    public void disableIfEnabled(UUID userId) {
        // Unconditional, unlike the disable below it: an admin resetting a user's 2FA means "clear
        // whatever they have", and an enrolment half-finished is something they have. Skipping it
        // used to answer 204 while leaving a pending enrolment confirmable — the admin was told it
        // had worked, and the user it was meant to unblock stayed stuck.
        pendingEnrolment.discard(userId);
        userTotpQuery.findById(userId).ifPresent(profile -> {
            if (profile.totpEnabled()) userTotpLifecyclePort.disableTotp(userId);
        });
    }
}