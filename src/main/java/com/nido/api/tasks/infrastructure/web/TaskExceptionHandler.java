package com.nido.api.tasks.infrastructure.web;

import com.nido.api.shared.infrastructure.web.ProblemDetailFactory;
import com.nido.api.tasks.domain.model.TaskException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class TaskExceptionHandler {

    @ExceptionHandler(TaskException.class)
    public ResponseEntity<ProblemDetail> handle(TaskException e, HttpServletRequest request) {
        TaskErrorResponse response = switch (e) {
            case TaskException.TaskNotFound ignored -> new TaskErrorResponse(404, "Task not found.");
            case TaskException.SubtasksIncomplete ignored ->
                new TaskErrorResponse(409, "All subtasks must be done before completing this task.");
            case TaskException.SameSpaceTransfer ignored ->
                new TaskErrorResponse(400, "Cannot transfer a task into its own context.");
            case TaskException.RecurringSeriesNotFound ignored -> new TaskErrorResponse(404, "Recurring series not found.");
            case TaskException.LeadTimeExceedsInterval ignored ->
                new TaskErrorResponse(400, "The lead time cannot exceed the recurrence interval.");
            case TaskException.InvalidEndDate ignored -> new TaskErrorResponse(400, "The end date must be on or after the anchor date.");
            case TaskException.MemberNotInSpace ignored -> new TaskErrorResponse(404, "Member is not part of this space.");
        };
        ProblemDetail problem = ProblemDetailFactory.of(
            HttpStatus.valueOf(response.status()), e.getClass().getSimpleName(), response.detail(),
            URI.create(request.getRequestURI()));
        return ResponseEntity.status(response.status()).body(problem);
    }

    private record TaskErrorResponse(int status, String detail) {}
}
