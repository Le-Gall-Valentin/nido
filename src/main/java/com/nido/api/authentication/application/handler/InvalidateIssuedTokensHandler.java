package com.nido.api.authentication.application.handler;

import com.nido.api.authentication.application.port.in.InvalidateIssuedTokensUseCase;
import com.nido.api.authentication.domain.port.out.IssuedTokenCutoffPort;
import com.nido.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@ApplicationService
public class InvalidateIssuedTokensHandler implements InvalidateIssuedTokensUseCase {

    private static final Logger log = LoggerFactory.getLogger(InvalidateIssuedTokensHandler.class);

    private final IssuedTokenCutoffPort issuedTokenCutoffPort;

    public InvalidateIssuedTokensHandler(IssuedTokenCutoffPort issuedTokenCutoffPort) {
        this.issuedTokenCutoffPort = issuedTokenCutoffPort;
    }

    @Override
    public void invalidateFor(UUID userId) {
        issuedTokenCutoffPort.cutOffNow(userId);
        log.info("Access tokens issued to user {} before now are no longer accepted", userId);
    }
}
