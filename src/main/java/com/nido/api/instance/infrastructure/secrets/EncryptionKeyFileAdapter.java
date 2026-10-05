package com.nido.api.instance.infrastructure.secrets;

import com.nido.api.infrastructure.config.DataDirectory;
import com.nido.api.instance.domain.port.out.KeyFilePort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class EncryptionKeyFileAdapter implements KeyFilePort {

    static final String NAME = "encryption-key";

    private final DataDirectory dataDirectory;

    public EncryptionKeyFileAdapter(DataDirectory dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    @Override
    public Optional<String> read() {
        return dataDirectory.readSecret(NAME);
    }

    @Override
    public String create() {
        return dataDirectory.createSecret(NAME);
    }

    @Override
    public String location() {
        return dataDirectory.secretPath(NAME).toString();
    }
}
