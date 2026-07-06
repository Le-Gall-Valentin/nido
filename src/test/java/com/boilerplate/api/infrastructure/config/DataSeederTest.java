package com.boilerplate.api.infrastructure.config;

import com.boilerplate.api.identity.application.port.in.SeedUseCase;
import com.boilerplate.api.identity.domain.model.IdentityException;
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
            .hasMessageContaining("BOILERPLATE_SEED_PASSWORD must be set");
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

    private BoilerplateProperties properties(String password) {
        return new BoilerplateProperties(
            new BoilerplateProperties.JwtProperties("test-secret-key-at-least-32-chars", 15, "boilerplate", "boilerplate"),
            new BoilerplateProperties.RefreshTokenProperties(30, "0 0 3 * * *"),
            new BoilerplateProperties.CookieProperties(false),
            new BoilerplateProperties.SeedProperties("user", "user@test.com", password),
            new BoilerplateProperties.CorsProperties(List.of()),
            new BoilerplateProperties.RateLimitProperties(List.of()),
            new BoilerplateProperties.EncryptionProperties("test-enc-secret"),
            null
        );
    }
}