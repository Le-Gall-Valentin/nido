package com.nido.api.infrastructure.sealing;

import java.util.regex.Pattern;

/** The one check that a name assembled into SQL is a plain identifier — names come from code, never from a request. */
public final class SqlIdentifier {

    private static final Pattern PLAIN = Pattern.compile("[a-z][a-z_]*");

    private SqlIdentifier() {}

    public static String require(String name) {
        if (name == null || !PLAIN.matcher(name).matches()) {
            throw new IllegalArgumentException("Not a plain SQL identifier: " + name);
        }
        return name;
    }
}
