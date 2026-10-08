package com.nido.api.shared.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * A way to prove, after the password, that a sign-in is the account holder's: the code of an
 * authenticator app, or a code sent by mail. Stored and sent by name.
 */
public enum TwoFactorMethod {
    APP,
    MAIL;

    /** How the method appears in a URL: {@code app}, {@code mail}. */
    public String pathSegment() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The method a URL names — lower case only, as {@link #pathSegment()} writes it. */
    public static Optional<TwoFactorMethod> fromPathSegment(String segment) {
        return Arrays.stream(values()).filter(method -> method.pathSegment().equals(segment)).findFirst();
    }

    /** The application before the mail, whatever order they came in: the order every response lists them in. */
    public static List<TwoFactorMethod> ordered(Collection<TwoFactorMethod> methods) {
        return Arrays.stream(values()).filter(methods::contains).toList();
    }
}
