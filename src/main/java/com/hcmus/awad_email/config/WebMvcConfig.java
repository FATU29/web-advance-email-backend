package com.hcmus.awad_email.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration
 * Registers interceptors for request logging
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Autowired
    private RequestLoggingInterceptor requestLoggingInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Register the request logging interceptor for all API endpoints
        registry.addInterceptor(requestLoggingInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/health"); // Exclude health check to reduce noise
    }
}

