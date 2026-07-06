package com.boilerplate.api.mfa.infrastructure.security;

import com.boilerplate.api.mfa.domain.port.out.TotpCodeValidatorPort;
import com.boilerplate.api.mfa.domain.port.out.TotpSecretGeneratorPort;
import com.boilerplate.api.mfa.domain.port.out.TotpUriBuilderPort;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class TotpServiceAdapter implements TotpSecretGeneratorPort, TotpCodeValidatorPort, TotpUriBuilderPort {

    private static final String ISSUER = "Boilerplate";
    private static final int SECRET_LENGTH = 32;

    private final DefaultSecretGenerator secretGenerator = new DefaultSecretGenerator(SECRET_LENGTH);
    private final DefaultCodeVerifier codeVerifier;

    public TotpServiceAdapter() {
        DefaultCodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA256, 6);
        SystemTimeProvider timeProvider = new SystemTimeProvider();
        this.codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        // Allow ±1 time window (30s each side = 90s total tolerance)
        this.codeVerifier.setAllowedTimePeriodDiscrepancy(1);
    }

    @Override
    public String generateSecret() {
        return secretGenerator.generate();
    }

    @Override
    public String buildOtpauthUri(String secret, String email) {
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8).replace("+", "%20");
        String encodedIssuer = URLEncoder.encode(ISSUER, StandardCharsets.UTF_8).replace("+", "%20");
        return "otpauth://totp/" + encodedIssuer + ":" + encodedEmail
            + "?secret=" + secret
            + "&issuer=" + encodedIssuer
            + "&algorithm=SHA256&digits=6&period=30";
    }

    @Override
    public boolean isValid(String secret, String code) {
        return codeVerifier.isValidCode(secret, code);
    }
}