package com.nido.api.identity.infrastructure.mail;

import com.nido.api.shared.model.TwoFactorMethod;

import java.util.Set;

/** What still protects the account after a reset. With two methods and one removed, at most one is left. */
public enum KeptMethod {
    NONE, APP, MAIL;

    public static KeptMethod of(Set<TwoFactorMethod> kept) {
        if (kept.contains(TwoFactorMethod.APP)) return APP;
        if (kept.contains(TwoFactorMethod.MAIL)) return MAIL;
        return NONE;
    }
}
