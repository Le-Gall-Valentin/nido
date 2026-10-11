package com.nido.api.infrastructure.encryption;

import org.springframework.security.crypto.encrypt.TextEncryptor;

/**
 * The key versions up to 0.15.x encrypted with — see {@link DataKeys#legacy}. It decrypts and nothing else: what is
 * written is always written with the current key.
 */
public final class LegacyDecryptor {

    private final TextEncryptor text;

    LegacyDecryptor(TextEncryptor text) {
        this.text = text;
    }

    public String decrypt(String encrypted) {
        return text.decrypt(encrypted);
    }
}
