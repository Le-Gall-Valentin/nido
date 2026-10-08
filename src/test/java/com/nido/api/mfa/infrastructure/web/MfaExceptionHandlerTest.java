package com.nido.api.mfa.infrastructure.web;

import com.nido.api.mfa.domain.model.MfaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class MfaExceptionHandlerTest {

    private MfaExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new MfaExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/2fa/mail/setup");
    }

    private static Object codeOf(ResponseEntity<ProblemDetail> response) {
        return response.getBody().getProperties() == null ? null : response.getBody().getProperties().get("error_code");
    }

    @Test
    void the_states_of_a_method_are_conflicts_named_for_the_client() {
        assertThat(codeOf(handler.handle(new MfaException.MethodAlreadyEnabled(), request))).isEqualTo("method_already_enabled");
        assertThat(codeOf(handler.handle(new MfaException.MethodNotEnabled(), request))).isEqualTo("method_not_enabled");
        var unavailable = handler.handle(new MfaException.MethodUnavailable(), request);
        assertThat(unavailable.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(codeOf(unavailable)).isEqualTo("method_unavailable");
    }

    @Test
    void asking_the_app_for_a_code_is_a_bad_request() {
        var response = handler.handle(new MfaException.MethodSendsNoCode(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(codeOf(response)).isEqualTo("method_sends_no_code");
    }

    @Test
    void a_code_sent_too_soon_says_when_to_ask_again() {
        var response = handler.handle(new MfaException.ResendTooSoon(42), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("42");
        assertThat(codeOf(response)).isEqualTo("resend_too_soon");
        assertThat(response.getBody().getProperties()).containsEntry("retryAfterSeconds", 42L);
    }

    @Test
    void the_mail_limit_says_when_to_ask_again() {
        var response = handler.handle(new MfaException.SendLimitReached(600), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("600");
        assertThat(codeOf(response)).isEqualTo("send_limit_reached");
    }

    @Test
    void the_answers_the_app_already_reads_do_not_change() {
        assertThat(handler.handle(new MfaException.UserNotFound(), request).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(handler.handle(new MfaException.EnrolmentNotStarted(), request).getStatusCode())
            .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(handler.handle(new MfaException.CodeInvalid(), request).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(handler.handle(new MfaException.InsufficientPermissions(), request).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        var lockout = handler.handle(new MfaException.ConfirmMaxAttemptsExceeded(), request);
        assertThat(lockout.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(lockout.getHeaders().containsHeader(HttpHeaders.RETRY_AFTER))
            .as("the client tells a lockout from a rate limit by the missing Retry-After").isFalse();
        assertThat(codeOf(lockout)).isNull();
    }
}
