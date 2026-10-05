package com.nido.api.instance.domain.port.out;

import java.util.Optional;

/** The encryption key Nido generated for itself, as a file of the data directory. */
public interface KeyFilePort {
    Optional<String> read();
    /** Generates a key, writes it, returns it. Never overwrites. */
    String create();
    /** Where the file is, for messages. */
    String location();
}
