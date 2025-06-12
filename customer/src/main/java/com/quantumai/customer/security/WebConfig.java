package com.quantumai.customer.security;

import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

// @Configuration

public class WebConfig {

  @Bean
  public CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();

    // Allow specific origins
    config.addAllowedOrigin("**");

    // Allow credentials
    config.setAllowCredentials(true);

    // Allow specific HTTP methods
    config.addAllowedMethod("GET");
    config.addAllowedMethod("POST");
    config.addAllowedMethod("PUT");
    config.addAllowedMethod("DELETE");
    config.addAllowedMethod("OPTIONS");

    // Allow custom headers
    config.addAllowedHeader("Content-Type");
    config.addAllowedHeader("Authorization");
    config.addAllowedHeader("Device-Id"); // Add your custom header
    config.addAllowedHeader("companyId");
    // Expose custom headers
    config.addExposedHeader("Device-Id");
    config.addExposedHeader("companyId");

    // Register configuration
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);

    return new CorsFilter(source);
  }
}
