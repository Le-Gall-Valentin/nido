package com.nido.api.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "nido")
public record NidoProperties(
    JwtProperties jwt,
    RefreshTokenProperties refreshToken,
    CookieProperties cookie,
    @Valid SeedProperties seed,
    CorsProperties cors,
    EncryptionProperties encryption,
    SecurityProperties security
) {

    /** The secret is optional: without one, a secret is generated in the data directory (JwtConfig). */
    public record JwtProperties(
        String secret,
        @DefaultValue("nido") String issuer,
        @DefaultValue("nido") String audience
    ) {}

    /** The lifetimes are instance settings now; only the purge schedule stays here. */
    public record RefreshTokenProperties(
        @DefaultValue("0 0 3 * * *") String purgeCron
    ) {}

    /** Null: decided by the public address (SessionSettingsAdapter). */
    public record CookieProperties(
        Boolean secure
    ) {}

    public record SeedProperties(
        @NotBlank String username,
        @NotBlank String email,
        @NotBlank @Size(min = 8, message = "Seed password must be at least 8 characters") String password
    ) {}

    public record CorsProperties(
        @DefaultValue("") List<String> allowedOrigins
    ) {}

    public record EncryptionProperties(String secret) {}

    public record SecurityProperties(
        @Positive @DefaultValue("15") int challengeTtlMinutes,
        /**
         * How long an account stays locked out of TOTP verification once it has used up its
         * attempts. Deliberately its own knob rather than reusing challengeTtlMinutes: one
         * governs how long a login may be left half-finished, the other how long brute-force
         * guessing is held off, and a future change to either must not silently move the other.
         */
        @Positive @DefaultValue("15") int totpLockoutMinutes,
        /**
         * How long an enrolment that has been started but never confirmed stays usable. Its own
         * knob again: this one bounds how long a QR code someone photographed over a shoulder
         * remains worth anything, which has nothing to do with how long a half-finished login may
         * be left open or how long guessing is held off.
         */
        @Positive @DefaultValue("15") int totpSetupTtlMinutes
    ) {}
}