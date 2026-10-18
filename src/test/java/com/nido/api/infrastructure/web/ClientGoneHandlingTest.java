package com.nido.api.infrastructure.web;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.catalina.connector.ClientAbortException;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ClientGoneHandlingTest {

    private final GlobalExceptionHandler handler =
        new GlobalExceptionHandler(mock(AuthenticationEntryPoint.class), mock(AccessDeniedHandler.class));

    @Test
    void a_client_that_went_away_mid_request_is_not_logged_as_an_error() {
        // An upload cancelled while the body is read: Tomcat's own exception, which nothing on our side caused.
        assertThat(errorsLoggedFor(new ClientAbortException(new IOException("Connection reset by peer")))).isEmpty();
    }

    @Test
    void any_other_failure_is_still_logged_as_an_error() {
        assertThat(errorsLoggedFor(new IllegalStateException("boom"))).hasSize(1);
    }

    private List<ILoggingEvent> errorsLoggedFor(Exception failure) {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> logged = new ListAppender<>();
        logged.start();
        logger.addAppender(logged);
        try {
            assertThat(handler.handleUnexpected(failure, new MockHttpServletRequest("POST", "/api/x")).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            return logged.list.stream().filter(event -> event.getLevel() == Level.ERROR).toList();
        } finally {
            logger.detachAppender(logged);
        }
    }
}
