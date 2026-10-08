package com.nido.api.identity.infrastructure.web;

import com.nido.api.identity.domain.model.IdentityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityExceptionHandlerTest {

    private IdentityExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new IdentityExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/users");
    }

    @Test
    void handle_userNotFound_returns404() {
        var response = handler.handle(new IdentityException.UserNotFound(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("UserNotFound");
    }

    @Test
    void handle_userNotActive_returns403() {
        var response = handler.handle(new IdentityException.UserNotActive(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("User account is not active.");
        assertThat(response.getBody().getTitle()).isEqualTo("UserNotActive");
    }

    @Test
    void handle_insufficientPermissions_returns403() {
        var response = handler.handle(new IdentityException.InsufficientPermissions(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Insufficient permissions.");
        assertThat(response.getBody().getTitle()).isEqualTo("InsufficientPermissions");
    }

    @Test
    void handle_userAlreadyInactive_returns409() {
        var response = handler.handle(new IdentityException.UserAlreadyInactive(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("UserAlreadyInactive");
    }

    @Test
    void handle_usernameAlreadyExists_returns409() {
        var response = handler.handle(new IdentityException.UsernameAlreadyExists(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("UsernameAlreadyExists");
    }

    @Test
    void handle_emailAlreadyExists_returns409() {
        var response = handler.handle(new IdentityException.EmailAlreadyExists(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("EmailAlreadyExists");
    }

    @Test
    void handle_dataIntegrityError_returns500WithGenericMessage() {
        var response = handler.handle(new IdentityException.DataIntegrityError(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("An unexpected error occurred. Please try again later.");
        assertThat(response.getBody().getTitle()).isEqualTo("DataIntegrityError");
    }

    @Test
    void handle_setsInstanceUri() {
        var response = handler.handle(new IdentityException.UserNotFound(), request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getInstance()).isNotNull();
        assertThat(response.getBody().getInstance().toString()).isEqualTo("/api/users");
    }

    @Test
    void a_wrong_address_code_is_a_bad_request_named_for_the_client_never_a_401() {
        var response = handler.handle(new IdentityException.EmailCodeInvalid(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getProperties()).containsEntry("error_code", "email_code_invalid");
    }

    @Test
    void a_spent_address_code_is_a_bad_request_named_for_the_client() {
        var response = handler.handle(new IdentityException.EmailCodeSpent(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getProperties()).containsEntry("error_code", "email_code_spent");
    }

    @Test
    void a_code_asked_again_too_soon_says_when_in_the_words_every_code_screen_reads() {
        var response = handler.handle(new IdentityException.EmailCodeResendTooSoon(40), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("40");
        assertThat(response.getBody().getProperties())
            .containsEntry("error_code", "resend_too_soon")
            .containsEntry("retryAfterSeconds", 40L);
    }

    @Test
    void the_account_send_limit_says_when_too() {
        var response = handler.handle(new IdentityException.EmailCodeSendLimitReached(420), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("420");
        assertThat(response.getBody().getProperties())
            .containsEntry("error_code", "send_limit_reached")
            .containsEntry("retryAfterSeconds", 420L);
    }

    @Test
    void an_expired_address_code_is_a_bad_request_named_apart() {
        var response = handler.handle(new IdentityException.EmailCodeExpired(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getProperties()).containsEntry("error_code", "email_code_expired");
    }
}
