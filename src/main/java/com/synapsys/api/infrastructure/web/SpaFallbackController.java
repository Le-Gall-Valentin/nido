package com.synapsys.api.infrastructure.web;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

@Hidden
@Controller
public class SpaFallbackController {

    @GetMapping(value = {"/{path:[^.]*}", "/**/{path:[^.]*}"})
    public String spa(HttpServletRequest request) {
        String path = request.getRequestURI()
                .substring(request.getContextPath().length());

        if (path.equals("/api") || path.startsWith("/api/")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        return "forward:/index.html";
    }
}