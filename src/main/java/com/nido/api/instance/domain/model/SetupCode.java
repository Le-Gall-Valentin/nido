package com.nido.api.instance.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;

/**
 * What proves, during the first-run setup, that whoever fills the screen has the server in hand: it is
 * written in the logs and nowhere else. 12 characters from an alphabet without 0/O or 1/I — about 60
 * bits — in memory only: a restart makes a new one, the end of the setup makes it useless.
 */
public record SetupCode(String value) {

    static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static SetupCode generate() {
        StringBuilder code = new StringBuilder(14);
        for (int i = 0; i < 12; i++) {
            if (i > 0 && i % 4 == 0) {
                code.append('-');
            }
            code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return new SetupCode(code.toString());
    }

    /** As read in a terminal and typed again: case, spaces and dashes do not matter. Constant time. */
    public boolean matches(String typed) {
        if (typed == null) {
            return false;
        }
        byte[] candidate = typed.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(candidate, value.replace("-", "").getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String toString() {
        return "SetupCode[***]";
    }
}
