package com.synapsys.api.identity.infrastructure.web;

import com.synapsys.api.identity.domain.model.IdentityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
}