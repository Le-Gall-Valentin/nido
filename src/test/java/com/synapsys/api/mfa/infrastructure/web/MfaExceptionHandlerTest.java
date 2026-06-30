package com.synapsys.api.mfa.infrastructure.web;

import com.synapsys.api.mfa.domain.model.MfaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class MfaExceptionHandlerTest {

    private MfaExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new MfaExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/2fa/setup");
    }

    @Test
    void handle_userNotFound_returns404() {
        var response = handler.handle(new MfaException.UserNotFound(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("UserNotFound");
    }

    @Test
    void handle_totpAlreadyEnabled_returns409() {
        var response = handler.handle(new MfaException.TotpAlreadyEnabled(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("TotpAlreadyEnabled");
    }

    @Test
    void handle_totpNotEnabled_returns409() {
        var response = handler.handle(new MfaException.TotpNotEnabled(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("TotpNotEnabled");
    }

    @Test
    void handle_totpSetupNotStarted_returns422() {
        var response = handler.handle(new MfaException.TotpSetupNotStarted(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("TotpSetupNotStarted");
    }

    @Test
    void handle_totpCodeInvalid_returns401() {
        var response = handler.handle(new MfaException.TotpCodeInvalid(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("TotpCodeInvalid");
    }

    @Test
    void handle_insufficientPermissions_returns403() {
        var response = handler.handle(new MfaException.InsufficientPermissions(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("InsufficientPermissions");
    }

    @Test
    void handle_totpConfirmMaxAttemptsExceeded_returns429() {
        var response = handler.handle(new MfaException.TotpConfirmMaxAttemptsExceeded(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("TotpConfirmMaxAttemptsExceeded");
    }

    @Test
    void handle_setsInstanceUri() {
        var response = handler.handle(new MfaException.TotpSetupNotStarted(), request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getInstance()).isNotNull();
        assertThat(response.getBody().getInstance().toString()).isEqualTo("/api/auth/2fa/setup");
    }
}