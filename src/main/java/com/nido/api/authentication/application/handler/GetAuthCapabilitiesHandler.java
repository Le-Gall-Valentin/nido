package com.nido.api.authentication.application.handler;

import com.nido.api.authentication.application.dto.AuthCapabilities;
import com.nido.api.authentication.application.port.in.GetAuthCapabilitiesUseCase;
import com.nido.api.authentication.domain.port.out.AccountMailPort;
import com.nido.api.shared.annotation.ApplicationService;

@ApplicationService
public class GetAuthCapabilitiesHandler implements GetAuthCapabilitiesUseCase {

    private final AccountMailPort accountMail;

    public GetAuthCapabilitiesHandler(AccountMailPort accountMail) {
        this.accountMail = accountMail;
    }

    @Override
    public AuthCapabilities capabilities() {
        boolean canSend = accountMail.canSend();
        return new AuthCapabilities(canSend, canSend);
    }
}
