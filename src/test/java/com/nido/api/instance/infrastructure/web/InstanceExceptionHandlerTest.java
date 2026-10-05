package com.nido.api.instance.infrastructure.web;

import com.nido.api.instance.domain.model.InstanceException;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.model.SettingProblem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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

    static Stream<Arguments> answers() {
        return Stream.of(
            Arguments.of(new InstanceException.SetupCodeInvalid(), 403, "SETUP_CODE_INVALID"),
            Arguments.of(new InstanceException.SetupAlreadyCompleted(), 404, null),
            Arguments.of(new InstanceException.UnknownSetting("nope"), 404, null),
            Arguments.of(new InstanceException.SettingsInvalid(List.of()), 400, "SETTINGS_INVALID"),
            Arguments.of(new InstanceException.SettingLockedByEnvironment(SettingKey.SWAGGER), 409, "SETTING_LOCKED_BY_ENVIRONMENT"),
            Arguments.of(new InstanceException.EncryptionKeyNotSaved(), 400, "ENCRYPTION_KEY_NOT_SAVED"),
            Arguments.of(new InstanceException.InitialAdminRefused("username"), 400, "INITIAL_ADMIN_REFUSED"));
    }

    @ParameterizedTest
    @MethodSource("answers")
    void each_refusal_has_its_status_and_the_code_the_pages_read(InstanceException refusal, int status, String errorCode) {
        ProblemDetail problem = handler.handle(refusal, request).getBody();

        assertThat(problem.getStatus()).isEqualTo(status);
        assertThat(problem.getProperties() == null ? null : problem.getProperties().get("error_code")).isEqualTo(errorCode);
    }

    @Test
    void the_problems_come_one_per_setting_and_a_lock_names_its_setting() {
        ProblemDetail invalid = handler.handle(new InstanceException.SettingsInvalid(List.of(
            new SettingProblem(SettingKey.MAIL_PORT, SettingProblem.NOT_A_NUMBER),
            new SettingProblem(SettingKey.MAIL_PORT, SettingProblem.OUT_OF_RANGE),
            new SettingProblem(SettingKey.MAIL_FROM, SettingProblem.REQUIRED))), request).getBody();
        ProblemDetail locked = handler.handle(new InstanceException.SettingLockedByEnvironment(SettingKey.SWAGGER), request).getBody();

        assertThat(invalid.getProperties()).containsEntry("errors",
            Map.of("mail.port", "not_a_number", "mail.from", "required"));
        assertThat(locked.getProperties()).containsEntry("setting", "api.swagger");
    }

    @Test
    void without_an_smtp_reply_there_is_no_reply_to_show() {
        ProblemDetail problem = handler.handle(new InstanceException.MailTestFailed("connection_refused", null), request).getBody();

        assertThat(problem.getProperties()).containsEntry("reason", "connection_refused").doesNotContainKey("server_reply");
    }
}
