package com.nido.api.instance.infrastructure.secrets;

import com.nido.api.instance.domain.port.out.ActiveEncryptionKeyPort;
import com.nido.api.shared.security.EncryptionKey;
import org.springframework.stereotype.Component;

@Component
public class ActiveEncryptionKeyAdapter implements ActiveEncryptionKeyPort {

    private final EncryptionKey encryptionKey;

    public ActiveEncryptionKeyAdapter(EncryptionKey encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    @Override
    public String value() {
        return encryptionKey.value();
    }
}
