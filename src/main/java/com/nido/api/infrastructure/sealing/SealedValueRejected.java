package com.nido.api.infrastructure.sealing;

import java.util.UUID;

/**
 * A stored value refused on reading: not where it was sealed, or not a sealed value at all. Names its place and
 * the reason — never the value.
 */
public class SealedValueRejected extends IllegalStateException {

    public enum Reason {
        NOT_SEALED("is not a sealed value"),
        UNDECRYPTABLE("does not decrypt"),
        MALFORMED("is not a readable envelope"),
        ELSEWHERE("belongs to another place");

        private final String wording;

        Reason(String wording) {
            this.wording = wording;
        }
    }

    private final Reason reason;

    SealedValueRejected(SealedColumn column, UUID rowId, Reason reason, String detail) {
        super("The value of " + column + " in row " + rowId + " " + reason.wording + (detail == null ? "" : " (" + detail + ")"));
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
