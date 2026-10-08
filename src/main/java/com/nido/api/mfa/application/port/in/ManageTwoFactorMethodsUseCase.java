package com.nido.api.mfa.application.port.in;

import com.nido.api.mfa.domain.model.EnrolmentStarted;
import com.nido.api.mfa.domain.model.MethodState;
import com.nido.api.shared.model.TwoFactorMethod;

import java.util.List;
import java.util.UUID;

/** What a signed-in person does with their own methods. */
public interface ManageTwoFactorMethodsUseCase {

    /** Every method, in order. */
    List<MethodState> methods(UUID userId);

    EnrolmentStarted startEnrolment(UUID userId, TwoFactorMethod method);

    void confirmEnrolment(UUID userId, TwoFactorMethod method, String code);

    /** @return the seconds before another code can be asked for */
    long sendDisableCode(UUID userId, TwoFactorMethod method);

    /** @param code the method's proof; may be null only for a paused method, which has none to give */
    void disable(UUID userId, TwoFactorMethod method, String code);
}
