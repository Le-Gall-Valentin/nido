package com.nido.api.infrastructure.config;

import java.util.Map;

/**
 * What a module still keeps in clear and must encrypt before the application serves anyone — see
 * {@link EncryptionBackfillRunner}. Declared by each module, over its own tables.
 */
public interface EncryptionBackfill {

    /** Whether any of the module's tables still holds a value in clear. */
    boolean pending();

    /** Encrypts it all; for each table, how many rows were rewritten. */
    Map<String, Integer> run();
}
