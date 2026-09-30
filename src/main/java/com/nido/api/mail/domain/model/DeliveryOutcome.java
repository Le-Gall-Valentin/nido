package com.nido.api.mail.domain.model;

/**
 * What one delivery attempt came to. A reason names exception classes only — never an address or
 * any content — because it is stored and logged.
 */
public sealed interface DeliveryOutcome {

    record Sent() implements DeliveryOutcome {}

    /** The server or the network may do better later: try again. */
    record TemporaryFailure(String reason) implements DeliveryOutcome {}

    /** Trying again would be refused the same way (unknown recipient, malformed address). */
    record PermanentFailure(String reason) implements DeliveryOutcome {}
}
