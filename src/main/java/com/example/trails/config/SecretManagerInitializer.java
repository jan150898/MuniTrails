package com.example.trails.config;

import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Load database credentials from environment variables at application startup.
 * Works on Fly.io, Cloud Run, and local development.
 */
@Component
public class SecretManagerInitializer implements ApplicationListener<ApplicationContextInitializedEvent> {

    @Override
    public void onApplicationEvent(ApplicationContextInitializedEvent event) {
        ConfigurableEnvironment env = event.getApplicationContext().getEnvironment();
        String activeProfile = env.getProperty("spring.profiles.active");
        
        if ("prod".equals(activeProfile)) {
            try {
                // Load from environment variables (Fly.io, Cloud Run, local)
                String dbHost = System.getenv("DB_HOST");
                String dbUsername = System.getenv("DB_USERNAME");
                String dbPassword = System.getenv("DB_PASSWORD");
                
                System.out.println("=== SecretManagerInitializer ===");
                System.out.println("DB_HOST: " + (dbHost != null ? "SET" : "NULL"));
                System.out.println("DB_USERNAME: " + (dbUsername != null ? "SET" : "NULL"));
                System.out.println("DB_PASSWORD: " + (dbPassword != null ? "SET" : "NULL"));
                
                if (dbHost == null || dbUsername == null || dbPassword == null) {
                    System.out.println("WARNING: Database credentials not fully set!");
                }
                
                // Add to Spring environment
                Map<String, Object> secrets = new HashMap<>();
                if (dbHost != null) secrets.put("DB_HOST", dbHost);
                if (dbUsername != null) secrets.put("DB_USERNAME", dbUsername);
                if (dbPassword != null) secrets.put("DB_PASSWORD", dbPassword);
                
                if (!secrets.isEmpty()) {
                    MapPropertySource propertySource = new MapPropertySource("secretsSource", secrets);
                    env.getPropertySources().addFirst(propertySource);
                    System.out.println("Secrets loaded into Spring environment");
                }
            } catch (Exception e) {
                System.err.println("ERROR loading secrets: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
