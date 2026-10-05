package com.nido.api.instance.infrastructure.web;

import com.nido.api.instance.application.port.in.GetEffectiveSettingsQuery;
import com.nido.api.instance.domain.model.SettingKey;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Swagger can be switched on and off from the settings page, so springdoc is always built and this
 * filter decides whether its routes answer. Registered after Spring Security, like any filter bean: an
 * anonymous caller still gets the 401 of /api/** before anything here runs.
 */
@Component
public class SwaggerToggleFilter extends OncePerRequestFilter {

    private final GetEffectiveSettingsQuery settings;

    public SwaggerToggleFilter(GetEffectiveSettingsQuery settings) {
        this.settings = settings;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !(path.equals("/api/docs") || path.startsWith("/api/docs/")
            || path.equals("/swagger-ui.html") || path.startsWith("/swagger-ui/"));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        if (settings.current().flag(SettingKey.SWAGGER)) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
}
