package com.nido.api.mfa.application.method;

import com.nido.api.mfa.domain.model.CodeCheck;
import com.nido.api.mfa.domain.model.CodeDelivery;
import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.mfa.domain.model.EnrolmentStarted;
import com.nido.api.mfa.domain.model.MfaException;
import com.nido.api.mfa.domain.port.out.AccountAddressPort;
import com.nido.api.mfa.domain.port.out.MailAvailabilityPort;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.shared.model.TwoFactorMethod;

import java.util.Optional;
import java.util.UUID;

/**
 * A code sent to the account's address. Turning it on proves the address receives mail; while mail is off the
 * method can be neither turned on nor asked for — it is paused, and the account signs in with its password.
 */
@ApplicationService
public class MailMethod implements TwoFactorMethodHandler {

    private final MailCodeIssuer issuer;
    private final MailAvailabilityPort availability;
    private final AccountAddressPort addresses;

    public MailMethod(MailCodeIssuer issuer, MailAvailabilityPort availability, AccountAddressPort addresses) {
        this.issuer = issuer;
        this.availability = availability;
        this.addresses = addresses;
    }

    @Override
    public TwoFactorMethod method() {
        return TwoFactorMethod.MAIL;
    }

    @Override
    public boolean usableNow() {
        return availability.isAvailable();
    }

    @Override
    public boolean deliversCodes() {
        return true;
    }

    @Override
    public EnrolmentStarted startEnrolment(UUID userId) {
        String address = addresses.addressOf(userId).orElseThrow(MfaException.UserNotFound::new);
        long resendAfter = issuer.issue(userId, CodePurpose.ENROL, userId.toString(), address).resendAfterOrThrow();
        return new EnrolmentStarted.MailEnrolment(address, resendAfter);
    }

    @Override
    public Optional<String> confirmEnrolment(UUID userId, String code) {
        if (!issuer.isPending(userId, CodePurpose.ENROL)) {
            throw new MfaException.EnrolmentNotStarted();
        }
        if (issuer.check(userId, CodePurpose.ENROL, userId.toString(), code) == CodeCheck.SUCCESS) {
            return Optional.empty();
        }
        // The fifth wrong code took the code with it: the enrolment has to start again.
        if (!issuer.isPending(userId, CodePurpose.ENROL)) {
            throw new MfaException.ConfirmMaxAttemptsExceeded();
        }
        throw new MfaException.CodeInvalid();
    }

    @Override
    public CodeDelivery sendCode(UUID userId, CodePurpose purpose, String binding) {
        return addresses.addressOf(userId)
            .map(address -> issuer.issue(userId, purpose, binding, address))
            .orElseGet(CodeDelivery.Unavailable::new);
    }

    @Override
    public CodeCheck check(UUID userId, CodePurpose purpose, String binding, String code) {
        return issuer.check(userId, purpose, binding, code);
    }

    @Override
    public void forgetPending(UUID userId) {
        issuer.forget(userId);
    }
}
