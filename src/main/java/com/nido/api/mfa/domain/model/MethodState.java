package com.nido.api.mfa.domain.model;

import com.nido.api.shared.model.TwoFactorMethod;

/** A method as its holder sees it. Enabled but not usable: paused — the mail method while mail is off. */
public record MethodState(TwoFactorMethod method, boolean enabled, boolean usable) {}
