package com.nido.api.instance.infrastructure.web;

import com.nido.api.instance.domain.model.InstanceException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class InstanceExceptionHandlerTest {

    private final InstanceExceptionHandler handler = new InstanceExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/settings/mail/test");

    @Test
    void a_failed_test_mail_says_why_and_carries_the_smtp_reply_apart() {
        ProblemDetail problem = handler.handle(new InstanceException.MailTestFailed("authentication_failed", "535 5.7.8 Bad credentials"), request).getBody();

        assertThat(problem.getStatus()).isEqualTo(422);
        assertThat(problem.getProperties())
            .containsEntry("error_code", "MAIL_TEST_FAILED")
            .containsEntry("reason", "authentication_failed")
            .containsEntry("server_reply", "535 5.7.8 Bad credentials");
    }

    @Test
    void without_an_smtp_reply_there_is_no_reply_to_show() {
        ProblemDetail problem = handler.handle(new InstanceException.MailTestFailed("connection_refused", null), request).getBody();

        assertThat(problem.getProperties()).containsEntry("reason", "connection_refused").doesNotContainKey("server_reply");
    }
}
