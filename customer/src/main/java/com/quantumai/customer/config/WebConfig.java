package com.quantumai.customer.config;

import com.quantumai.customer.interceptor.RateLimitingInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitingInterceptor rateLimitingInterceptor;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        log.info("Registering rate limiting interceptor");
        registry.addInterceptor(rateLimitingInterceptor)
                .addPathPatterns(
                    "/api/**",
                    "/customer/**",
                    "/admin/**"
                )
                .excludePathPatterns(
                    "/error",
                    "/actuator/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-resources/**"
                );
        log.info("Rate limiting interceptor registered successfully");
    }
}
