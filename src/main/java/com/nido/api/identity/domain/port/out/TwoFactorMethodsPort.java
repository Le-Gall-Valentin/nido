package com.nido.api.identity.domain.port.out;

import com.nido.api.shared.model.TwoFactorMethod;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** The second factors of accounts — mfa's, read and changed through this port. */
public interface TwoFactorMethodsPort {

    /** On, paused ones included. */
    Set<TwoFactorMethod> activeMethods(UUID userId);

    /** Every account asked for is a key. */
    Map<UUID, Set<TwoFactorMethod>> activeMethodsAmong(Collection<UUID> userIds);

    /** @return what was on and is now off */
    Set<TwoFactorMethod> removeByAdmin(UUID userId, Set<TwoFactorMethod> methods);

    void deleteAll(UUID userId);
}
