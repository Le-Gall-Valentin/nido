package com.nido.api.mfa.application.handler;

import com.nido.api.mfa.application.method.MailCodeIssuer;
import com.nido.api.mfa.application.port.in.AddressChangeCodeUseCase;
import com.nido.api.mfa.domain.model.CodeCheck;
import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.mfa.domain.port.out.MailAvailabilityPort;
import com.nido.api.mfa.domain.port.out.TwoFactorMethodStorePort;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.shared.model.TwoFactorMethod;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class AddressChangeCodeHandler implements AddressChangeCodeUseCase {

    private final TwoFactorMethodStorePort store;
    private final MailAvailabilityPort availability;
    private final MailCodeIssuer issuer;

    public AddressChangeCodeHandler(TwoFactorMethodStorePort store, MailAvailabilityPort availability, MailCodeIssuer issuer) {
        this.store = store;
        this.availability = availability;
        this.issuer = issuer;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean required(UUID userId) {
        return store.activeMethods(userId).contains(TwoFactorMethod.MAIL) && availability.isAvailable();
    }

    @Override
    @Transactional
    public long send(UUID userId, String newAddress) {
        return issuer.issue(userId, CodePurpose.EMAIL_CHANGE, newAddress, newAddress).resendAfterOrThrow();
    }

    @Override
    @Transactional
    public boolean check(UUID userId, String newAddress, String code) {
        return issuer.check(userId, CodePurpose.EMAIL_CHANGE, newAddress, code) == CodeCheck.SUCCESS;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean pending(UUID userId) {
        return issuer.isPending(userId, CodePurpose.EMAIL_CHANGE);
    }
}
