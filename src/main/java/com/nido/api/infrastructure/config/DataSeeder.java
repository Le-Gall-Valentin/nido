package com.nido.api.infrastructure.config;

import com.nido.api.identity.application.port.in.SeedUseCase;
import com.nido.api.identity.domain.model.IdentityException;
import com.nido.api.identity.domain.model.Username;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;


@Component
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final NidoProperties properties;
    private final SeedUseCase seedUseCase;

    public DataSeeder(NidoProperties properties, SeedUseCase seedUseCase) {
        this.properties = properties;
        this.seedUseCase = seedUseCase;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        NidoProperties.SeedProperties seed = properties.seed();
        if (seed == null || seed.password() == null || seed.password().isBlank()) {
            throw new IllegalStateException(
                "NIDO_SEED_PASSWORD must be set — the initial SUPER_ADMIN cannot be created without it"
            );
        }
        if (!Username.isValid(seed.username())) {
            throw new IllegalStateException(
                "NIDO_SEED_USERNAME is not a username Nido accepts: 3 to 50 characters, no @"
            );
        }
        try {
            seedUseCase.seedInitialSuperAdmin(seed.username(), seed.email(), seed.password());
        } catch (IdentityException.UsernameAlreadyExists | IdentityException.EmailAlreadyExists e) {
            log.info("SUPER_ADMIN already exists (concurrent startup), skipping");
        }
    }
}