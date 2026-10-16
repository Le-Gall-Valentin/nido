package com.nido.api.calendar.application.port.in;

import java.util.Optional;

public interface ReadSubscriptionFeedUseCase {
    /** The calendar an address reads; empty for an address that does not exist, is revoked, or whose owner left. */
    Optional<String> read(String token);
}
