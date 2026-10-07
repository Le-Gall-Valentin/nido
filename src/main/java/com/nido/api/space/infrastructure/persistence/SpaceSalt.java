package com.nido.api.space.infrastructure.persistence;

import java.security.SecureRandom;
import java.util.HexFormat;

/** A space's encryption salt: random, drawn once with the space, never derived from anything visible. */
public final class SpaceSalt {

    private static final SecureRandom RANDOM = new SecureRandom();

    private SpaceSalt() {}

    public static String random() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
