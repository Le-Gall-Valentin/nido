package com.nido.api.mfa.domain.model;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * How a mail code is kept. A six-digit code is found in a moment from a plain hash, so the code is an HMAC
 * keyed by what it is bound to. For a sign-in that is the challenge id, which lives only in Redis and the
 * browser's cookie: reading the database gives neither the code nor a way to use it. JDK primitives only.
 */
public final class MailCodeDigest {

    private static final HexFormat HEX = HexFormat.of();

    private MailCodeDigest() {}

    /** HMAC-SHA-256 of the code, keyed by what it is bound to, in hex. */
    public static String of(String binding, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(binding.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HEX.formatHex(mac.doFinal(code.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }

    /** SHA-256 of what a code is bound to: tells a resend for the same thing from a new request. */
    public static String bindingHash(String binding) {
        try {
            return HEX.formatHex(MessageDigest.getInstance("SHA-256").digest(binding.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /** Compared in constant time. */
    public static boolean matches(String expectedHex, String binding, String code) {
        return MessageDigest.isEqual(
            expectedHex.getBytes(StandardCharsets.US_ASCII),
            of(binding, code).getBytes(StandardCharsets.US_ASCII));
    }
}
