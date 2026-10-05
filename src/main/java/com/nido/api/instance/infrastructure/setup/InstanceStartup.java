package com.nido.api.instance.infrastructure.setup;

import com.nido.api.infrastructure.config.NidoProperties;
import com.nido.api.instance.application.port.in.StartInstanceUseCase;
import com.nido.api.instance.domain.model.EnvironmentSeed;
import com.nido.api.instance.domain.model.SetupCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Once every bean is ready, and before the web server takes a single request: refuses a wrong
 * configuration in the environment, sets the installation up from NIDO_SEED_* when they are given, and
 * otherwise prints the setup code — in a box, so that it stands out of a `docker compose logs`. Run any
 * later, a setup screen opened at once would ask for a code that does not exist yet. A refusal throws,
 * and the application does not start.
 */
@Component
public class InstanceStartup implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(InstanceStartup.class);

    private final StartInstanceUseCase start;
    private final NidoProperties properties;

    public InstanceStartup(StartInstanceUseCase start, NidoProperties properties) {
        this.start = start;
        this.properties = properties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        NidoProperties.SeedProperties seed = properties.seed();
        EnvironmentSeed environmentSeed = seed == null
            ? new EnvironmentSeed(null, null, null)
            : new EnvironmentSeed(seed.username(), seed.email(), seed.password());
        start.start(environmentSeed).ifPresent(code -> log.warn("\n{}", banner(code)));
    }

    static String banner(SetupCode code) {
        String[] lines = {"Nido is not set up yet.", "Setup code: " + code.value(), "Open Nido in a browser and enter it."};
        int width = Arrays.stream(lines).mapToInt(String::length).max().orElse(0) + 4;
        StringBuilder box = new StringBuilder("╔").append("═".repeat(width)).append("╗\n");
        for (String line : lines) {
            box.append("║  ").append(line).append(" ".repeat(width - line.length() - 2)).append("║\n");
        }
        return box.append("╚").append("═".repeat(width)).append("╝").toString();
    }
}
