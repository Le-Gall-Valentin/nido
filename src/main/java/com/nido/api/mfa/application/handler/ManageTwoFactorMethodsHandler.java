package com.nido.api.mfa.application.handler;

import com.nido.api.mfa.application.method.TwoFactorMethodHandler;
import com.nido.api.mfa.application.method.TwoFactorMethods;
import com.nido.api.mfa.application.port.in.ManageTwoFactorMethodsUseCase;
import com.nido.api.mfa.domain.model.CodeCheck;
import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.mfa.domain.model.EnrolmentStarted;
import com.nido.api.mfa.domain.model.MethodState;
import com.nido.api.mfa.domain.model.MfaException;
import com.nido.api.mfa.domain.port.out.TwoFactorMailPort;
import com.nido.api.mfa.domain.port.out.TwoFactorMethodStorePort;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.shared.model.TwoFactorMethod;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Turning methods on and off, written once for every method. What each method does differently — how it
 * starts, how its codes are made and checked — is its handler's business; the rules are the same for all:
 * nothing is turned on that is on or cannot be used now, nothing is turned off without its proof unless it is
 * paused, and the holder is told either way.
 */
@ApplicationService
public class ManageTwoFactorMethodsHandler implements ManageTwoFactorMethodsUseCase {

    private final TwoFactorMethods methods;
    private final TwoFactorMethodStorePort store;
    private final TwoFactorMailPort mails;

    public ManageTwoFactorMethodsHandler(TwoFactorMethods methods, TwoFactorMethodStorePort store, TwoFactorMailPort mails) {
        this.methods = methods;
        this.store = store;
        this.mails = mails;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MethodState> methods(UUID userId) {
        Set<TwoFactorMethod> active = store.activeMethods(userId);
        return Arrays.stream(TwoFactorMethod.values())
            .map(method -> new MethodState(method, active.contains(method), methods.of(method).usableNow()))
            .toList();
    }

    @Override
    @Transactional
    public EnrolmentStarted startEnrolment(UUID userId, TwoFactorMethod method) {
        return usableAndOff(userId, method).startEnrolment(userId);
    }

    /**
     * A wrong code must stay counted: the mail method keeps its failures next to the code, in this transaction,
     * and rolling it back with the error would leave the five-tries limit counting nothing.
     */
    @Override
    @Transactional(noRollbackFor = {MfaException.CodeInvalid.class, MfaException.ConfirmMaxAttemptsExceeded.class})
    public void confirmEnrolment(UUID userId, TwoFactorMethod method, String code) {
        TwoFactorMethodHandler handler = usableAndOff(userId, method);
        Optional<String> secret = handler.confirmEnrolment(userId, code);
        // The moment the enrolment becomes what protects the account: written where it survives, then forgotten.
        store.enable(userId, method, secret.orElse(null));
        handler.forgetPending(userId);
        mails.methodEnabled(userId, method);
    }

    @Override
    @Transactional
    public long sendDisableCode(UUID userId, TwoFactorMethod method) {
        TwoFactorMethodHandler handler = methods.of(method);
        if (!handler.deliversCodes()) {
            throw new MfaException.MethodSendsNoCode();
        }
        if (!store.activeMethods(userId).contains(method)) {
            throw new MfaException.MethodNotEnabled();
        }
        return handler.sendCode(userId, CodePurpose.DISABLE, userId.toString()).resendAfterOrThrow();
    }

    /** Same reason as {@link #confirmEnrolment}: a wrong code is counted, so the error does not undo the count. */
    @Override
    @Transactional(noRollbackFor = MfaException.CodeInvalid.class)
    public void disable(UUID userId, TwoFactorMethod method, String code) {
        Set<TwoFactorMethod> active = store.activeMethods(userId);
        if (!active.contains(method)) {
            throw new MfaException.MethodNotEnabled();
        }
        TwoFactorMethodHandler handler = methods.of(method);
        // A paused method protects nothing and has no way to prove itself: it goes on the session alone.
        if (handler.usableNow()) {
            if (code == null || handler.check(userId, CodePurpose.DISABLE, userId.toString(), code) != CodeCheck.SUCCESS) {
                throw new MfaException.CodeInvalid();
            }
        }
        store.disable(userId, method);
        handler.forgetPending(userId);
        mails.methodDisabled(userId, method, active.size() > 1);
    }

    private TwoFactorMethodHandler usableAndOff(UUID userId, TwoFactorMethod method) {
        if (store.activeMethods(userId).contains(method)) {
            throw new MfaException.MethodAlreadyEnabled();
        }
        TwoFactorMethodHandler handler = methods.of(method);
        if (!handler.usableNow()) {
            throw new MfaException.MethodUnavailable();
        }
        return handler;
    }
}
