package com.nido.api.instance.infrastructure.web;

import com.nido.api.instance.application.port.in.GetEffectiveSettingsQuery;
import com.nido.api.instance.domain.model.SettingKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Swagger can be switched on and off from the settings page, so springdoc is always built and this
 * decides whether its routes answer.
 *
 * <p>An MVC interceptor, not a servlet filter comparing the raw URI: its patterns are matched against
 * the path exactly as MVC matches its handlers, so no other spelling of a documentation route gets
 * past it — {@code /api/docs.yaml}, {@code /api/docs;x=1}. It runs after Spring Security: an anonymous
 * caller still gets the 401 of /api/** first.
 */
@Configuration(proxyBeanMethods = false)
public class SwaggerToggle implements WebMvcConfigurer {

    private final GetEffectiveSettingsQuery settings;

    public SwaggerToggle(GetEffectiveSettingsQuery settings) {
        this.settings = settings;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                     @NonNull Object handler) {
                if (settings.current().flag(SettingKey.SWAGGER)) {
                    return true;
                }
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return false;
            }
        }).addPathPatterns("/api/docs", "/api/docs.*", "/api/docs/**", "/swagger-ui.html", "/swagger-ui/**");
    }
}
