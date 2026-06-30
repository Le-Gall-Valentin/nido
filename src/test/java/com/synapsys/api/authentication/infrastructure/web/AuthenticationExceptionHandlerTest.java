package com.synapsys.api.authentication.infrastructure.web;

import com.synapsys.api.authentication.domain.model.AuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationExceptionHandlerTest {

    private AuthenticationExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new AuthenticationExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");
    }

    @Test
    void handle_invalidCredentials_returns401() {
        var response = handler.handle(new AuthenticationException.InvalidCredentials(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Invalid credentials");
    }

    @Test
    void handle_userNotActive_returns401WithGenericMessage() {
        var response = handler.handle(new AuthenticationException.UserNotActive(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Authentication required");
        assertThat(response.getBody().getTitle()).isEqualTo("AuthenticationError");
    }

    @Test
    void handle_userNotFound_returns401WithGenericMessage() {
        var response = handler.handle(new AuthenticationException.UserNotFound(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Authentication required");
        assertThat(response.getBody().getTitle()).isEqualTo("AuthenticationError");
    }

    @Test
    void handle_tokenExpired_returns401WithGenericMessage() {
        var response = handler.handle(new AuthenticationException.TokenExpired(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Authentication required");
    }

    @Test
    void handle_tokenNotFound_returns401WithGenericMessage() {
        var response = handler.handle(new AuthenticationException.TokenNotFound(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Authentication required");
    }

    @Test
    void handle_tokenRevoked_returns401WithGenericMessage() {
        var response = handler.handle(new AuthenticationException.TokenRevoked(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Authentication required");
    }

    @Test
    void handle_totpCodeInvalid_returns401WithGenericMessage() {
        var response = handler.handle(new AuthenticationException.TotpCodeInvalid(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Authentication required");
        assertThat(response.getBody().getTitle()).isEqualTo("AuthenticationError");
    }

    @Test
    void handle_totpChallengeExpired_returns401WithStableErrorCode() {
        var response = handler.handle(new AuthenticationException.TotpChallengeExpired(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Authentication required");
        assertThat(response.getBody().getTitle()).isEqualTo("AuthenticationError");
        assertThat(response.getBody().getProperties()).containsEntry("error_code", "totp_challenge_expired");
    }

    @Test
    void handle_totpMaxAttemptsExceeded_returns429() {
        var response = handler.handle(new AuthenticationException.TotpMaxAttemptsExceeded(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Authentication required");
        assertThat(response.getBody().getTitle()).isEqualTo("AuthenticationError");
    }

    @Test
    void handle_setsInstanceUri() {
        var response = handler.handle(new AuthenticationException.InvalidCredentials(), request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getInstance()).isNotNull();
        assertThat(response.getBody().getInstance().toString()).isEqualTo("/api/auth/login");
    }
}