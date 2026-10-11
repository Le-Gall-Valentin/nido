package com.nido.api.instance.application.handler;

import com.nido.api.instance.application.port.in.CloseLegacyFormatsUseCase;
import com.nido.api.instance.application.port.in.LegacyFormatsQuery;
import com.nido.api.instance.domain.port.out.InstanceStatePort;
import com.nido.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/** Whether the installation still takes values of a format before 0.16.0 — see LegacyFormats. */
@ApplicationService
public class LegacyFormatsHandler implements LegacyFormatsQuery, CloseLegacyFormatsUseCase {

    private static final Logger log = LoggerFactory.getLogger(LegacyFormatsHandler.class);

    private final InstanceStatePort instanceState;
    private final Clock clock;

    public LegacyFormatsHandler(InstanceStatePort instanceState, Clock clock) {
        this.instanceState = instanceState;
        this.clock = clock;
    }

    @Override
    public boolean closed() {
        return instanceState.legacyFormatsClosed();
    }

    @Override
    @Transactional
    public void closeForGood() {
        if (instanceState.closeLegacyFormats(Instant.now(clock))) {
            log.info("Every encrypted value is of the current format: values of an earlier format are refused from now on");
        }
    }
}
