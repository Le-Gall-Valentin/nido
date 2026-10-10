package com.nido.api.infrastructure.encryption;

import com.nido.api.infrastructure.sealing.Envelope;
import com.nido.api.infrastructure.sealing.SealedColumn;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.UUID;

/** Writes data the way versions up to 0.15.x did: for tests that start from such data, and only them. */
public final class LegacyKeys {

    private LegacyKeys() {}

    /** What Encryptors.delux(masterKey, hexSalt) was, encrypting included. */
    public static TextEncryptor writer(String masterKey, String hexSalt) {
        return new HexTextEncryptor(DataKeys.legacyBytes(masterKey, hexSalt));
    }

    /** A value as 0.14.0 to 0.15.x sealed it: v2: and its envelope, under the legacy key. */
    public static String sealedV2(String masterKey, String hexSalt, SealedColumn column, UUID rowId, String value) {
        return "v2:" + writer(masterKey, hexSalt).encrypt(Envelope.wrap(column.referenceFor(rowId), value));
    }
}
