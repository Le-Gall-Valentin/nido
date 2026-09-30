package com.nido.api.authentication.application.port.in;

import com.nido.api.authentication.application.dto.AuthCapabilities;

public interface GetAuthCapabilitiesUseCase {
    AuthCapabilities capabilities();
}
