package com.nido.api.authentication.infrastructure.web;

import com.nido.api.authentication.application.port.in.GetAuthCapabilitiesUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.server.ResponseStatusException;

/**
 * "Forgot password" answers 404 while mail is off, as if its routes did not exist — mail can be switched
 * on and off without a restart, so they can no longer simply not be registered. Checked before the
 * handler runs: a body that does not validate must not answer 400 and give the routes away, and a
 * refused call must not spend the caller's rate limit.
 */
@Configuration(proxyBeanMethods = false)
public class PasswordResetAvailability implements WebMvcConfigurer {

    private final GetAuthCapabilitiesUseCase capabilities;

    public PasswordResetAvailability(GetAuthCapabilitiesUseCase capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                     @NonNull Object handler) {
                if (!capabilities.capabilities().passwordReset()) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND);
                }
                return true;
            }
        }).addPathPatterns("/api/auth/password-reset/**");
    }
}
