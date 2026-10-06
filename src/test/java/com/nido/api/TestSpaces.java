package com.nido.api;

import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import org.springframework.security.crypto.encrypt.Encryptors;

/**
 * A space row written straight through JPA — the way many tests set one up — with its name encrypted as
 * the application would: SpaceRepositoryAdapter is not there to do it.
 */
public final class TestSpaces {

    /** The master key every test context runs with — see IntegrationTestConfig. */
    public static final String ENCRYPTION_KEY = "integration-test-encryption-secret-32chars!";

    private TestSpaces() {}

    /** Gives the space a salt if it has none yet, then its name, encrypted with that salt and the test key. */
    public static void name(SpaceEntity space, String name) {
        if (space.getEncryptionSalt() == null) {
            space.setEncryptionSalt(SpaceEntity.newEncryptionSalt());
        }
        space.setNameEncrypted(Encryptors.delux(ENCRYPTION_KEY, space.getEncryptionSalt()).encrypt(name));
    }
}
