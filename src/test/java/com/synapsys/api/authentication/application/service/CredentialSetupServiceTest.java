package com.synapsys.api.authentication.application.service;

import com.synapsys.api.authentication.domain.port.out.PasswordHasherPort;
import com.synapsys.api.authentication.domain.port.out.UserCredentialPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialSetupServiceTest {

    @Mock PasswordHasherPort passwordHasher;
    @Mock UserCredentialPort userCredentialPort;

    private CredentialSetupService service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CredentialSetupService(passwordHasher, userCredentialPort);
    }

    @Test
    void setup_hashesPasswordAndSavesCredential() {
        when(passwordHasher.hash("rawPassword")).thenReturn("hashedPassword");

        service.setup(userId, "rawPassword");

        verify(passwordHasher).hash("rawPassword");
        verify(userCredentialPort).saveCredential(userId, "hashedPassword");
    }

    @Test
    void setup_delegatesHashToHasher_notRawPassword() {
        when(passwordHasher.hash("secret")).thenReturn("$2a$12$hashed");

        service.setup(userId, "secret");

        verify(userCredentialPort).saveCredential(userId, "$2a$12$hashed");
    }
}