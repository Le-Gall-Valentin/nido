package com.nido.api.infrastructure.config;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.UUID;
import java.util.function.Function;

/** Decrypts one value per space, read by a query that returns {@code space_id} and {@code value}. */
public final class SpaceCiphertextCheck implements ExistingCiphertextCheck {

    private final JdbcClient jdbc;
    private final Function<UUID, TextEncryptor> keyOfSpace;
    private final String what;
    private final String oneValuePerSpace;

    public SpaceCiphertextCheck(JdbcClient jdbc, Function<UUID, TextEncryptor> keyOfSpace, String what,
                                String oneValuePerSpace) {
        this.jdbc = jdbc;
        this.keyOfSpace = keyOfSpace;
        this.what = what;
        this.oneValuePerSpace = oneValuePerSpace;
    }

    @Override
    public void verify() {
        jdbc.sql(oneValuePerSpace)
            .query((rs, rowNum) -> new Sample(rs.getObject("space_id", UUID.class), rs.getString("value")))
            .list()
            .forEach(this::decrypt);
    }

    private void decrypt(Sample sample) {
        try {
            keyOfSpace.apply(sample.spaceId()).decrypt(sample.value());
        } catch (RuntimeException e) {
            throw new IllegalStateException("The encryption key does not decrypt the " + what
                + " already encrypted in space " + sample.spaceId() + ": nothing was encrypted with it. "
                + "Start with the key this database was encrypted with.");
        }
    }

    private record Sample(UUID spaceId, String value) {}
}
