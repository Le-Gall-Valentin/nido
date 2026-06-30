package com.synapsys.api.infrastructure.config;

import com.synapsys.api.identity.application.port.in.SeedUseCase;
import com.synapsys.api.identity.domain.model.IdentityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final SynapsysProperties properties;
    private final SeedUseCase seedUseCase;

    public DataSeeder(SynapsysProperties properties, SeedUseCase seedUseCase) {
        this.properties = properties;
        this.seedUseCase = seedUseCase;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        SynapsysProperties.SeedProperties seed = properties.seed();
        if (seed == null || seed.password() == null || seed.password().isBlank()) {
            throw new IllegalStateException(
                "SYNAPSYS_SEED_PASSWORD must be set — the initial SUPER_ADMIN cannot be created without it"
            );
        }
        try {
            seedUseCase.seedInitialSuperAdmin(seed.username(), seed.email().toLowerCase(Locale.ROOT), seed.password());
        } catch (IdentityException.UsernameAlreadyExists | IdentityException.EmailAlreadyExists e) {
            log.info("SUPER_ADMIN already exists (concurrent startup), skipping");
        }
    }
}