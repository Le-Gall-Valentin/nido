package com.nido.api.mfa.application.port.in;

import com.nido.api.shared.model.TwoFactorMethod;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** The methods accounts have on — paused ones included: they are on, only not asked for. */
public interface GetTwoFactorMethodsUseCase {

    Set<TwoFactorMethod> activeMethods(UUID userId);

    /** Every account asked for is a key. */
    Map<UUID, Set<TwoFactorMethod>> activeMethodsAmong(Collection<UUID> userIds);
}
