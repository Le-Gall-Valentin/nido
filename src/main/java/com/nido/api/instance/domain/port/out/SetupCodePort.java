package com.nido.api.instance.domain.port.out;

import com.nido.api.instance.domain.model.SetupCode;

import java.util.Optional;

public interface SetupCodePort {
    void store(SetupCode code);
    Optional<SetupCode> current();
}
