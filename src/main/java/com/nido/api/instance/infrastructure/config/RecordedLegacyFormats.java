package com.nido.api.instance.infrastructure.config;

import com.nido.api.infrastructure.sealing.LegacyFormats;
import com.nido.api.instance.application.port.in.CloseLegacyFormatsUseCase;
import com.nido.api.instance.application.port.in.LegacyFormatsQuery;

/**
 * Whether earlier formats are still taken, as the instance row records it — see LegacyFormatsHandler. Asked at start,
 * and for each value read without the current prefix: a query each during the one start that converts them — a few
 * dozen rows of three small stores — and none once they are closed, for that answer is kept: closed, they never open
 * again. "Still open" is not kept: another instance may close them in between.
 */
public class RecordedLegacyFormats implements LegacyFormats {

    private final LegacyFormatsQuery query;
    private final CloseLegacyFormatsUseCase closing;
    private volatile boolean closed;

    public RecordedLegacyFormats(LegacyFormatsQuery query, CloseLegacyFormatsUseCase closing) {
        this.query = query;
        this.closing = closing;
    }

    @Override
    public boolean closed() {
        if (!closed && query.closed()) {
            closed = true;
        }
        return closed;
    }

    @Override
    public void closeForGood() {
        closing.closeForGood();
        closed = true;
    }
}
