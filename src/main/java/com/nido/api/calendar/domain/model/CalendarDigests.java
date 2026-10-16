package com.nido.api.calendar.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * What the calendar keeps of a value it must recognise but never show: the token of a subscription address, the UID of
 * an imported event. SHA-256, as the session tokens: a lookup by digest costs the same whatever was asked.
 */
public final class CalendarDigests {

    private CalendarDigests() {}

    public static String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
