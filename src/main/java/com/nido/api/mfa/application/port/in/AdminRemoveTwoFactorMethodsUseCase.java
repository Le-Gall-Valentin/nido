package com.nido.api.mfa.application.port.in;

import com.nido.api.shared.model.TwoFactorMethod;

import java.util.Set;
import java.util.UUID;

public interface AdminRemoveTwoFactorMethodsUseCase {

    /**
     * Removes the methods asked for. An enrolment under way for one of them is cleared too, whatever its state:
     * an administrator resetting a method means "clear whatever they have", and a half-finished enrolment is
     * something they have.
     *
     * @return the methods that were on and are now off — what makes the reset worth telling
     */
    Set<TwoFactorMethod> remove(UUID userId, Set<TwoFactorMethod> methods);
}
