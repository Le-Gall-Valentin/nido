package com.synapsys.api.infrastructure.config;

import com.synapsys.api.identity.application.port.in.SeedUseCase;
import com.synapsys.api.identity.domain.model.IdentityException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock SeedUseCase seedUseCase;

    @Test
    void seed_withoutPassword_throws() {
        DataSeeder seeder = new DataSeeder(properties(" "), seedUseCase);
        assertThatThrownBy(seeder::seed)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SYNAPSYS_SEED_PASSWORD must be set");
        verifyNoInteractions(seedUseCase);
    }

    @Test
    void seed_withPassword_delegatesToSeedUseCase() {
        DataSeeder seeder = new DataSeeder(properties("secret"), seedUseCase);
        assertThatNoException().isThrownBy(seeder::seed);
        verify(seedUseCase).seedInitialSuperAdmin("user", "user@test.com", "secret");
    }

    @Test
    void seed_usernameAlreadyExists_completesNormally() {
        doThrow(new IdentityException.UsernameAlreadyExists())
            .when(seedUseCase).seedInitialSuperAdmin(anyString(), anyString(), anyString());

        DataSeeder seeder = new DataSeeder(properties("secret"), seedUseCase);
        assertThatNoException().isThrownBy(seeder::seed);
    }

    @Test
    void seed_emailAlreadyExists_completesNormally() {
        doThrow(new IdentityException.EmailAlreadyExists())
            .when(seedUseCase).seedInitialSuperAdmin(anyString(), anyString(), anyString());

        DataSeeder seeder = new DataSeeder(properties("secret"), seedUseCase);
        assertThatNoException().isThrownBy(seeder::seed);
    }

    @Test
    void seed_infraFailure_propagatesException() {
        doThrow(new RuntimeException("DB unavailable"))
            .when(seedUseCase).seedInitialSuperAdmin(anyString(), anyString(), anyString());

        DataSeeder seeder = new DataSeeder(properties("secret"), seedUseCase);
        assertThatThrownBy(seeder::seed)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("DB unavailable");
    }

    private SynapsysProperties properties(String password) {
        return new SynapsysProperties(
            new SynapsysProperties.JwtProperties("test-secret-key-at-least-32-chars", 15, "synapsys-api", "synapsys-api"),
            new SynapsysProperties.RefreshTokenProperties(30, "0 0 3 * * *"),
            new SynapsysProperties.CookieProperties(false),
            new SynapsysProperties.SeedProperties("user", "user@test.com", password),
            new SynapsysProperties.CorsProperties(List.of()),
            new SynapsysProperties.RateLimitProperties(List.of()),
            new SynapsysProperties.EncryptionProperties("test-enc-secret"),
            null
        );
    }
}