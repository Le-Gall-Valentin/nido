package com.nido.api.instance.infrastructure.setup;

import com.nido.api.instance.domain.model.SetupCode;
import com.nido.api.instance.domain.port.out.SetupCodePort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** In memory only: a restart makes a new code, and nothing ever writes one to disk or to the database. */
@Component
public class InMemorySetupCodeAdapter implements SetupCodePort {

    private final AtomicReference<SetupCode> code = new AtomicReference<>();

    @Override
    public void store(SetupCode code) {
        this.code.set(code);
    }

    @Override
    public Optional<SetupCode> current() {
        return Optional.ofNullable(code.get());
    }
}
