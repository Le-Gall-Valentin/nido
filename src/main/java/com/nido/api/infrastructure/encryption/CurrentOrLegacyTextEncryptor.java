package com.nido.api.infrastructure.encryption;

import com.nido.api.shared.security.EncryptionKey;
import org.springframework.security.crypto.encrypt.TextEncryptor;

/**
 * The encryptor of the values encrypted under a key of their own rather than a space's — two-factor secrets, the SMTP
 * password, the mail queue. Writes with the current key only, behind {@value #PREFIX}; reads that, and reads what has no
 * prefix — what versions up to 0.15.x wrote — with the legacy key.
 *
 * <p>Why read both: the setting store is read at start by InstanceStartup, in no set order with the migration that
 * brings these values to the current key (EncryptionBackfillRunner). After the first start of 0.16.0 nothing is left
 * for the fallback; it goes with the legacy key. A {@value #PREFIX} value that does not open is refused: it is never
 * tried with the legacy key.
 */
public final class CurrentOrLegacyTextEncryptor implements TextEncryptor {

    public static final String PREFIX = "k2:";

    private final TextEncryptor current;
    private final LegacyDecryptor legacy;

    CurrentOrLegacyTextEncryptor(TextEncryptor current, LegacyDecryptor legacy) {
        this.current = current;
        this.legacy = legacy;
    }

    public static CurrentOrLegacyTextEncryptor of(EncryptionKey key, String hexSalt) {
        return new CurrentOrLegacyTextEncryptor(DataKeys.current(key, hexSalt), DataKeys.legacy(key, hexSalt));
    }

    public static boolean isCurrent(String stored) {
        return stored.startsWith(PREFIX);
    }

    @Override
    public String encrypt(String text) {
        return PREFIX + current.encrypt(text);
    }

    @Override
    public String decrypt(String stored) {
        return isCurrent(stored) ? current.decrypt(stored.substring(PREFIX.length())) : legacy.decrypt(stored);
    }
}
