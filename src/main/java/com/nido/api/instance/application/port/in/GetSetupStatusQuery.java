package com.nido.api.instance.application.port.in;

import com.nido.api.instance.domain.model.SetupStatus;

public interface GetSetupStatusQuery {
    SetupStatus status();
}
