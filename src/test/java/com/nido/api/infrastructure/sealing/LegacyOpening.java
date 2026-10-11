package com.nido.api.infrastructure.sealing;

import java.util.UUID;

/** Opens what a legacy opener reads, for the test of a module that provides one: otherwise only the migration does. */
public final class LegacyOpening {

    private LegacyOpening() {}

    public static String open(LegacySpaceOpener opener, SealedColumn column, UUID rowId, String stored) {
        return opener.open(column, rowId, stored);
    }
}
