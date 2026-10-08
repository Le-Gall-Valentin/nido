package com.nido.api.mfa.domain.port.out;

import com.nido.api.shared.model.TwoFactorMethod;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** The methods each account has turned on — what protects it once the password is right. */
public interface TwoFactorMethodStorePort {

    /** Application before mail; empty when the account has none. */
    Set<TwoFactorMethod> activeMethods(UUID userId);

    /** Every account asked for is a key, mapped to an empty set when it has no method. */
    Map<UUID, Set<TwoFactorMethod>> activeMethodsAmong(Collection<UUID> userIds);

    /** The authenticator secret, decrypted; empty when the application method is off. */
    Optional<String> appSecret(UUID userId);

    /**
     * Turns a method on.
     *
     * @param secret the authenticator secret in clear for {@link TwoFactorMethod#APP} — encrypted here —
     *               and null for {@link TwoFactorMethod#MAIL}
     */
    void enable(UUID userId, TwoFactorMethod method, String secret);

    /** @return whether the method was on */
    boolean disable(UUID userId, TwoFactorMethod method);

    void deleteAll(UUID userId);
}
