package com.nido.api.infrastructure.encryption;

import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.codec.Utf8;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.TextEncryptor;

/**
 * Text in and out of a {@link BytesEncryptor}: UTF-8 bytes in, hex-encoded ciphertext out — what Encryptors.delux wrapped
 * around its cipher, and what Spring Security no longer offers undeprecated. Encoding only: no key, no cipher.
 */
final class HexTextEncryptor implements TextEncryptor {

    private final BytesEncryptor bytes;

    HexTextEncryptor(BytesEncryptor bytes) {
        this.bytes = bytes;
    }

    @Override
    public String encrypt(String text) {
        return new String(Hex.encode(bytes.encrypt(Utf8.encode(text))));
    }

    @Override
    public String decrypt(String encryptedText) {
        return Utf8.decode(bytes.decrypt(Hex.decode(encryptedText)));
    }
}
