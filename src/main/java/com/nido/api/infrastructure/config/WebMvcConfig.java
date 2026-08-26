package com.nido.api.infrastructure.config;

import com.nido.api.infrastructure.web.SpaceMembershipArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final SpaceMembershipArgumentResolver spaceMembershipArgumentResolver;

    public WebMvcConfig(SpaceMembershipArgumentResolver spaceMembershipArgumentResolver) {
        this.spaceMembershipArgumentResolver = spaceMembershipArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(spaceMembershipArgumentResolver);
    }
}
