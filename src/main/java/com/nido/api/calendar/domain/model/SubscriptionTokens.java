package com.nido.api.calendar.domain.model;

import java.util.regex.Pattern;

/** The secret of a subscription address: 32 random bytes, in base64url so it travels in a path as it is. */
public final class SubscriptionTokens {

    private static final Pattern SHAPE = Pattern.compile("[A-Za-z0-9_-]{43}");

    private SubscriptionTokens() {}

    /** Whether {@code token} could have been made here: anything else is not looked up at all. */
    public static boolean wellFormed(String token) {
        return token != null && SHAPE.matcher(token).matches();
    }
}
