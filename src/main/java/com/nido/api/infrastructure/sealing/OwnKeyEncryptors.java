package com.nido.api.infrastructure.sealing;

import com.nido.api.infrastructure.encryption.CurrentOrLegacyTextEncryptor;
import com.nido.api.shared.security.EncryptionKey;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

/**
 * The encryptors of the values under a key of their own rather than a space's — two-factor secrets, the SMTP password,
 * the mail queue — for the modules that keep them: written with the current key, and what versions up to 0.15.x wrote
 * read with the legacy key for as long as earlier formats are taken (CurrentOrLegacyTextEncryptor, LegacyFormats). A new
 * one at each call: the module decides how long it lives — see EncryptorCache.
 */
@Component
public class OwnKeyEncryptors {

    private final EncryptionKey key;
    private final LegacyFormats legacyFormats;

    public OwnKeyEncryptors(EncryptionKey key, LegacyFormats legacyFormats) {
        this.key = key;
        this.legacyFormats = legacyFormats;
    }

    /** @param hexSalt what sets this key apart from the others derived from the master key — not a secret */
    public TextEncryptor forSalt(String hexSalt) {
        return CurrentOrLegacyTextEncryptor.of(key, hexSalt, () -> !legacyFormats.closed());
    }
}
