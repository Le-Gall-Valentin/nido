package com.nido.api.instance.application.port.in;

import com.nido.api.instance.domain.model.EnvironmentSeed;
import com.nido.api.instance.domain.model.SetupCode;

import java.util.Optional;

/**
 * What a start does once the application is up: refuse a wrong configuration in the environment, set
 * the installation up from NIDO_SEED_* when given, and otherwise issue the setup code. Throws to stop the start.
 */
public interface StartInstanceUseCase {
    Optional<SetupCode> start(EnvironmentSeed seed);
}
