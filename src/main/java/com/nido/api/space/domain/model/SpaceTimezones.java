package com.nido.api.space.domain.model;

import java.time.ZoneId;

/**
 * Turns what a browser reported into the zone a space keeps, or refuses it.
 *
 * <p>This is the one value on a space that no human typed: the frontend reads it from
 * {@code Intl.DateTimeFormat().resolvedOptions().timeZone} and sends it along. Stored unchecked it
 * does not fail on the way in — it fails on every later read of that space, for every member, until
 * somebody edits the row by hand.
 *
 * <p>Region identifiers only, even though {@code ZoneId.of} would happily accept {@code +02:00}: a
 * fixed offset does not follow daylight saving, so a household stored that way is an hour wrong for
 * half the year and nobody would connect the two.
 */
public final class SpaceTimezones {

    private SpaceTimezones() {}

    public static ZoneId parseOrThrow(String timezone) {
        if (!ZoneId.getAvailableZoneIds().contains(timezone)) {
            throw new SpaceException.InvalidTimezone();
        }
        return ZoneId.of(timezone);
    }
}
