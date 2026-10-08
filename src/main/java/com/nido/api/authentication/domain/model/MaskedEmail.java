package com.nido.api.authentication.domain.model;

/**
 * An address shown to someone who has only typed the right password: enough to recognise their own mailbox,
 * not enough to learn someone else's. Always six dots, so the length of the address does not show.
 */
public final class MaskedEmail {

    private static final String DOTS = "••••••";

    private MaskedEmail() {}

    public static String of(String address) {
        int at = address.lastIndexOf('@');
        if (at <= 0) {
            return DOTS;
        }
        String local = address.substring(0, at);
        String first = local.substring(0, local.offsetByCodePoints(0, 1));
        String last = local.codePointCount(0, local.length()) >= 3
            ? local.substring(local.offsetByCodePoints(local.length(), -1))
            : "";
        return first + DOTS + last + address.substring(at);
    }
}
