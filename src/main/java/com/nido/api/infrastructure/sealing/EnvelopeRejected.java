package com.nido.api.infrastructure.sealing;

import java.util.Optional;

/** A text that is not an envelope, or an envelope of another place — whose reference it then gives, never its value. */
public final class EnvelopeRejected extends IllegalArgumentException {

    private final String claimedReference;

    private EnvelopeRejected(String message, String claimedReference) {
        super(message);
        this.claimedReference = claimedReference;
    }

    static EnvelopeRejected malformed() {
        return new EnvelopeRejected("not a readable envelope", null);
    }

    static EnvelopeRejected elsewhere(String claimedReference) {
        return new EnvelopeRejected("sealed for " + claimedReference, claimedReference);
    }

    public Optional<String> claimedReference() {
        return Optional.ofNullable(claimedReference);
    }
}
