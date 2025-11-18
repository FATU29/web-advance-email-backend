package com.hcmus.awad_email.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class to load environment variables from .env file
 * This runs before Spring Boot loads application.yml
 */
public class DotenvConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        
        try {
            // Load .env file from project root
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")
                    .ignoreIfMissing() // Don't fail if .env doesn't exist
                    .load();
            
            // Convert dotenv entries to a Map
            Map<String, Object> dotenvMap = new HashMap<>();
            dotenv.entries().forEach(entry -> {
                dotenvMap.put(entry.getKey(), entry.getValue());
            });
            
            // Add dotenv properties to Spring Environment with high priority
            environment.getPropertySources().addFirst(
                    new MapPropertySource("dotenvProperties", dotenvMap)
            );
            
            System.out.println("✓ Environment variables loaded from .env file");
            
        } catch (Exception e) {
            System.out.println("⚠ Warning: Could not load .env file. Using system environment variables or defaults.");
            System.out.println("  Reason: " + e.getMessage());
        }
    }
}

