package com.nido.api.calendar.domain.port.out;

import java.util.Optional;

/** The address nido answers at from outside, as an administrator set it: what a subscription address begins with. */
public interface PublicAddressPort {
    /** Without a trailing '/': {@code https://nido.example}. Empty when none is set, or none valid. */
    Optional<String> publicUrl();
}
