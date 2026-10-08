package com.nido.api.authentication.infrastructure.web;

import com.nido.api.authentication.domain.model.AuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
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
        assertThat(response.getBody().getDetail()).isEqualTo("Invalid credentials");
        assertThat(response.getBody().getTitle()).isEqualTo("AuthenticationError");
    }

    @Test
    void handle_userNotActive_isIndistinguishableFromInvalidCredentials() {
        // A wrong-password attempt and a right-password-but-disabled-account attempt must produce
        // byte-identical responses, or an attacker probing passwords against a known username could
        // tell the exact moment they guessed right from a change in the response body.
        var invalidCredentials = handler.handle(new AuthenticationException.InvalidCredentials(), request);
        var userNotActive = handler.handle(new AuthenticationException.UserNotActive(), request);

        assertThat(userNotActive.getStatusCode()).isEqualTo(invalidCredentials.getStatusCode());
        assertThat(userNotActive.getBody()).isNotNull();
        assertThat(invalidCredentials.getBody()).isNotNull();
        assertThat(userNotActive.getBody().getTitle()).isEqualTo(invalidCredentials.getBody().getTitle());
        assertThat(userNotActive.getBody().getDetail()).isEqualTo(invalidCredentials.getBody().getDetail());
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
        var response = handler.handle(new AuthenticationException.TwoFactorCodeInvalid(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Authentication required");
        assertThat(response.getBody().getTitle()).isEqualTo("AuthenticationError");
    }

    @Test
    void handle_totpChallengeExpired_returns401WithStableErrorCode() {
        var response = handler.handle(new AuthenticationException.TwoFactorChallengeExpired(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Authentication required");
        assertThat(response.getBody().getTitle()).isEqualTo("AuthenticationError");
        assertThat(response.getBody().getProperties()).containsEntry("error_code", "two_factor_challenge_expired");
    }

    @Test
    void handle_totpMaxAttemptsExceeded_returns429() {
        var response = handler.handle(new AuthenticationException.TwoFactorMaxAttemptsExceeded(), request);

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

    @Test
    void handle_invalidResetToken_returns410WithStableErrorCode() {
        var response = handler.handle(new AuthenticationException.InvalidResetToken(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProperties()).containsEntry("error_code", "reset_link_invalid");
    }

    @Test
    void handle_accountAlreadyJoined_returns409() {
        var response = handler.handle(new AuthenticationException.AccountAlreadyJoined(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("This account already chose its password.");
        assertThat(response.getBody().getProperties()).containsEntry("error_code", "account_already_joined");
    }

    @Test
    void a_refused_mail_code_says_which_and_when_to_ask_again() {
        var tooSoon = handler.handle(new AuthenticationException.MailCodeRefused(true, 30), request);
        var limit = handler.handle(new AuthenticationException.MailCodeRefused(false, 420), request);

        assertThat(tooSoon.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(tooSoon.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("30");
        assertThat(tooSoon.getBody().getProperties()).containsEntry("error_code", "resend_too_soon");
        assertThat(limit.getBody().getProperties()).containsEntry("error_code", "send_limit_reached");
    }

    @Test
    void a_method_off_or_paused_is_a_conflict_named_for_the_client() {
        assertThat(handler.handle(new AuthenticationException.MethodNotEnabled(), request).getBody().getProperties())
            .containsEntry("error_code", "method_not_enabled");
        assertThat(handler.handle(new AuthenticationException.MethodUnavailable(), request).getBody().getProperties())
            .containsEntry("error_code", "method_unavailable");
    }

    @Test
    void a_lockout_at_sign_in_says_when_to_come_back() {
        var response = handler.handle(new AuthenticationException.TwoFactorLockedOut(600), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("600");
        assertThat(response.getBody().getProperties()).containsEntry("error_code", "two_factor_locked");
    }
}
