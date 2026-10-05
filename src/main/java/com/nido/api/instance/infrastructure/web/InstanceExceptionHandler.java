package com.nido.api.instance.infrastructure.web;

import com.nido.api.instance.domain.model.InstanceException;
import com.nido.api.instance.domain.model.SettingProblem;
import com.nido.api.shared.infrastructure.web.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Ahead of GlobalExceptionHandler's last resort, which matches Exception and would otherwise be
// picked first on an order tie — turning a declared domain error into a 500.
@Order(Ordered.LOWEST_PRECEDENCE - 100)
@RestControllerAdvice
public class InstanceExceptionHandler {

    private record Answer(HttpStatus status, String errorCode, String detail) {}

    @ExceptionHandler(InstanceException.class)
    public ResponseEntity<ProblemDetail> handle(InstanceException e, HttpServletRequest request) {
        Answer answer = switch (e) {
            case InstanceException.SetupCodeInvalid ex ->
                new Answer(HttpStatus.FORBIDDEN, "SETUP_CODE_INVALID", "The setup code is not the one in the logs.");
            case InstanceException.SetupAlreadyCompleted ex ->
                new Answer(HttpStatus.NOT_FOUND, null, "This installation is already set up.");
            case InstanceException.UnknownSetting ex ->
                new Answer(HttpStatus.NOT_FOUND, null, ex.getMessage());
            case InstanceException.SettingsInvalid ex ->
                new Answer(HttpStatus.BAD_REQUEST, "SETTINGS_INVALID", "Some settings are not valid.");
            case InstanceException.SettingLockedByEnvironment ex ->
                new Answer(HttpStatus.CONFLICT, "SETTING_LOCKED_BY_ENVIRONMENT", ex.getMessage());
            case InstanceException.MailTestFailed ex ->
                new Answer(HttpStatus.valueOf(422), "MAIL_TEST_FAILED",
                    ex.serverReply() != null ? ex.serverReply() : "The test mail could not be sent.");
            case InstanceException.EncryptionKeyNotSaved ex ->
                new Answer(HttpStatus.BAD_REQUEST, "ENCRYPTION_KEY_NOT_SAVED", ex.getMessage());
            case InstanceException.InitialAdminRefused ex ->
                new Answer(HttpStatus.BAD_REQUEST, "INITIAL_ADMIN_REFUSED", ex.getMessage());
        };
        ProblemDetail problem = ProblemDetailFactory.of(answer.status(), e.getClass().getSimpleName(), answer.detail(),
            URI.create(request.getRequestURI()));
        if (answer.errorCode() != null) {
            problem.setProperty("error_code", answer.errorCode());
        }
        if (e instanceof InstanceException.SettingsInvalid invalid) {
            problem.setProperty("errors", errors(invalid.problems()));
        }
        if (e instanceof InstanceException.MailTestFailed failed) {
            problem.setProperty("reason", failed.reason());
            if (failed.serverReply() != null) {
                problem.setProperty("server_reply", failed.serverReply());
            }
        }
        if (e instanceof InstanceException.SettingLockedByEnvironment locked) {
            problem.setProperty("setting", locked.key().code());
        }
        return ResponseEntity.status(answer.status()).body(problem);
    }

    /** One problem per setting, the first found: the page shows one message under each field. */
    static Map<String, String> errors(List<SettingProblem> problems) {
        Map<String, String> errors = new LinkedHashMap<>();
        problems.forEach(problem -> errors.putIfAbsent(problem.key().code(), problem.code()));
        return errors;
    }
}
