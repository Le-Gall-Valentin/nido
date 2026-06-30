package com.synapsys.api.shared.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

public final class ProblemDetailFactory {

    private ProblemDetailFactory() {}

    public static ProblemDetail of(HttpStatus status, String title, String detail, URI instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(instance);
        return problem;
    }
}