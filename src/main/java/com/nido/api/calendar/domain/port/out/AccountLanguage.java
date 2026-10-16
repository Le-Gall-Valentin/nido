package com.nido.api.calendar.domain.port.out;

import java.util.Locale;
import java.util.UUID;

/** The language an account reads nido in — the words an export adds are written in it. */
public interface AccountLanguage {
    Locale of(UUID userId);
}
