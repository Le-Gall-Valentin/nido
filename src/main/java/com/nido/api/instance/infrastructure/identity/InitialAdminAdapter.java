package com.nido.api.instance.infrastructure.identity;

import com.nido.api.identity.application.port.in.SeedUseCase;
import com.nido.api.identity.domain.model.IdentityException;
import com.nido.api.instance.domain.model.InitialAdmin;
import com.nido.api.instance.domain.model.InstanceException;
import com.nido.api.instance.domain.port.out.InitialAdminPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class InitialAdminAdapter implements InitialAdminPort {

    private final SeedUseCase seed;

    public InitialAdminAdapter(SeedUseCase seed) {
        this.seed = seed;
    }

    @Override
    public Optional<UUID> create(InitialAdmin admin) {
        try {
            return seed.seedInitialSuperAdmin(admin.username(), admin.email(), admin.password(), admin.language());
        } catch (IdentityException.InvalidUsername e) {
            throw new InstanceException.InitialAdminRefused("username");
        }
    }
}
