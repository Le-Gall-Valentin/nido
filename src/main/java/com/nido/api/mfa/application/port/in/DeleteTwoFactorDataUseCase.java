package com.nido.api.mfa.application.port.in;

import java.util.UUID;

public interface DeleteTwoFactorDataUseCase {

    /** Every method, every code sent, every enrolment under way: erasing means all of it, now. */
    void deleteUserData(UUID userId);
}
