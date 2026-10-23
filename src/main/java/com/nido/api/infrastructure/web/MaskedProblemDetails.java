package com.nido.api.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * A problem answering a path LoggedPath masks — a subscription address, whose path is its secret — never names it,
 * whichever handler wrote it: its instance is the path as LoggedPath says it, and a detail naming the path (Spring's own
 * "No static resource …") says it masked too. Each handler need not remember.
 */
// It answers no exception itself, but it is an advice like the others: ahead of GlobalExceptionHandler's last resort.
@Order(Ordered.LOWEST_PRECEDENCE - 100)
@RestControllerAdvice
public class MaskedProblemDetails implements ResponseBodyAdvice<Object> {

    private static final Pattern SUBSCRIPTION_PATH = Pattern.compile("ical/[^\\s/]+");

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
                                  Class<? extends HttpMessageConverter<?>> converterType, ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (body instanceof ProblemDetail problem && request instanceof ServletServerHttpRequest servlet) {
            HttpServletRequest raw = servlet.getServletRequest();
            String logged = LoggedPath.of(raw);
            if (!logged.equals(raw.getRequestURI())) {
                problem.setInstance(URI.create(logged));
                if (problem.getDetail() != null) {
                    problem.setDetail(SUBSCRIPTION_PATH.matcher(problem.getDetail()).replaceAll(logged.substring(1)));
                }
            }
        }
        return body;
    }
}
