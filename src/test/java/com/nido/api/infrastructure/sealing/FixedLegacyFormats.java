package com.nido.api.infrastructure.sealing;

/** Earlier formats taken or refused for good, whatever the database says: for tests that need one state or the other. */
public final class FixedLegacyFormats {

    private FixedLegacyFormats() {}

    /** The state of an installation until its first start of 0.16.0 has brought every value to the current format. */
    public static LegacyFormats open() {
        return of(false);
    }

    /** The state of every installation once that start is over. */
    public static LegacyFormats closed() {
        return of(true);
    }

    private static LegacyFormats of(boolean closed) {
        return new LegacyFormats() {
            @Override
            public boolean closed() {
                return closed;
            }

            @Override
            public void closeForGood() {
                throw new AssertionError("a test of the earlier formats' state closes nothing");
            }
        };
    }
}
