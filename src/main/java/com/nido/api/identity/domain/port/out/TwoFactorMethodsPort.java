package com.nido.api.identity.domain.port.out;

import com.nido.api.shared.model.TwoFactorMethod;

import java.util.Set;
import java.util.UUID;

/** The second factors of accounts — mfa's, read through this port. */
public interface TwoFactorMethodsPort {

    /** On, paused ones included. */
    Set<TwoFactorMethod> activeMethods(UUID userId);
}
