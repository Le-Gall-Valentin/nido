package com.nido.api.instance.application.port.in;

import java.util.Optional;

/** The key every encryptor of the application uses, decided once at start; throws to stop the start. */
public interface ResolveEncryptionKeyUseCase {
    String resolve(Optional<String> configuredKey);
}
