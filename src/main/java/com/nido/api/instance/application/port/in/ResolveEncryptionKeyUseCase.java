package com.nido.api.instance.application.port.in;

import com.nido.api.instance.domain.model.ResolvedEncryptionKey;

/** The key every encryptor of the application uses, decided once at start; throws to stop the start. */
public interface ResolveEncryptionKeyUseCase {
    /** {@code configuredKey}: NIDO_ENCRYPTION_SECRET, or null — blank counts as not given. */
    ResolvedEncryptionKey resolve(String configuredKey);
}
