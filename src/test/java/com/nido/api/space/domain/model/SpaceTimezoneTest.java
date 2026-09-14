package com.nido.api.space.domain.model;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A space's timezone is the one field here that arrives from a browser rather than from a form the
 * user filled in, so it is the one that can carry a value nobody chose. It has to be refused at the
 * boundary: stored unchecked, it does not fail here — it fails on every later read of the space,
 * for everyone, until somebody edits the row by hand.
 */
class SpaceTimezoneTest {

    private UpdateSpaceCommand withTimezone(String timezone) {
        return new UpdateSpaceCommand(UUID.randomUUID(), null, null, null, null,
            timezone == null ? null : SpaceTimezones.parseOrThrow(timezone));
    }

    @Test
    void a_zone_the_browser_reported_is_accepted_as_it_is() {
        // Intl.DateTimeFormat().resolvedOptions().timeZone returns exactly this shape, which is also
        // what ZoneId parses — there is nothing to translate between the two.
        assertThat(withTimezone("America/Toronto").timezone()).isEqualTo(ZoneId.of("America/Toronto"));
    }

    @Test
    void a_zone_that_names_nowhere_is_refused_at_the_boundary() {
        assertThatThrownBy(() -> withTimezone("Europe/Atlantis"))
            .isInstanceOf(SpaceException.InvalidTimezone.class);
    }

    @Test
    void a_fixed_offset_is_refused_even_though_java_would_parse_it() {
        // ZoneId.of("+02:00") is valid Java and wrong here: an offset does not follow daylight
        // saving, so a household stored that way drifts by an hour twice a year.
        assertThatThrownBy(() -> withTimezone("+02:00"))
            .isInstanceOf(SpaceException.InvalidTimezone.class);
    }

    @Test
    void leaving_the_zone_out_keeps_whatever_the_space_already_had() {
        // Partial update: every other field here behaves this way, and changing a household's
        // calendar must be something somebody asked for, never a side effect of renaming it.
        assertThat(withTimezone(null).timezone()).isNull();
    }
}
