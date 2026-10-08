package com.nido.api.identity.infrastructure.mail;

import com.nido.api.shared.model.TwoFactorMethod;

import java.util.Set;

/** What a reset took away, as the mails name it. */
public enum ResetMethods {
    APP, MAIL, BOTH;

    public static ResetMethods of(Set<TwoFactorMethod> removed) {
        if (removed.contains(TwoFactorMethod.APP) && removed.contains(TwoFactorMethod.MAIL)) return BOTH;
        if (removed.contains(TwoFactorMethod.APP)) return APP;
        if (removed.contains(TwoFactorMethod.MAIL)) return MAIL;
        throw new IllegalArgumentException("A reset that removed nothing is not told");
    }
}
