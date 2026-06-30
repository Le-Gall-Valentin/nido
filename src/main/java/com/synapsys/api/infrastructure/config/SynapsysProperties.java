package com.synapsys.api.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "synapsys")
public record SynapsysProperties(
    @Valid JwtProperties jwt,
    RefreshTokenProperties refreshToken,
    CookieProperties cookie,
    @Valid SeedProperties seed,
    CorsProperties cors,
    RateLimitProperties rateLimit,
    @Valid EncryptionProperties encryption,
    SecurityProperties security
) {

    public record JwtProperties(
        @NotBlank String secret,
        @Positive @DefaultValue("15") int expiryMinutes,
        @DefaultValue("synapsys-api") String issuer,
        @DefaultValue("synapsys-api") String audience
    ) {}

    public record RefreshTokenProperties(
        @DefaultValue("30") int expiryDays,
        @DefaultValue("0 0 3 * * *") String purgeCron
    ) {}

    public record CookieProperties(
        @DefaultValue("true") boolean secure
    ) {}

    public record SeedProperties(
        @NotBlank String username,
        @NotBlank String email,
        @NotBlank @Size(min = 8, message = "Seed password must be at least 8 characters") String password
    ) {}

    public record CorsProperties(
        @DefaultValue("") List<String> allowedOrigins
    ) {}

    public record RateLimitProperties(
        @DefaultValue("") List<String> trustedProxies
    ) {}

    public record EncryptionProperties(
        @NotBlank @Size(min = 32, message = "Encryption secret must be at least 32 characters for sufficient entropy") String secret
    ) {}

    public record SecurityProperties(
        @Positive @DefaultValue("15") int challengeTtlMinutes
    ) {}
}