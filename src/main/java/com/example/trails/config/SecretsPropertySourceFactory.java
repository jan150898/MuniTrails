package com.example.trails.config;

import org.springframework.boot.context.properties.source.ConfigurationPropertySourcesPropertyResolver;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Custom PropertySourceFactory to load database credentials from environment variables
 * at the earliest stage of property resolution (before datasource bean creation).
 */
public class SecretsPropertySourceFactory implements PropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(@Nullable String name, org.springframework.core.io.Resource resource) throws IOException {
        return new SecretsPropertySource(name != null ? name : "secrets", loadSecretsFromEnvironment());
    }

    private Map<String, Object> loadSecretsFromEnvironment() {
        Map<String, Object> secrets = new HashMap<>();
        
        String dbHost = System.getenv("DB_HOST");
        String dbUsername = System.getenv("DB_USERNAME");
        String dbPassword = System.getenv("DB_PASSWORD");
        
        if (dbHost != null && !dbHost.isEmpty()) {
            secrets.put("spring.datasource.url", "jdbc:postgresql://" + dbHost + ":5432/postgres");
        }
        if (dbUsername != null && !dbUsername.isEmpty()) {
            secrets.put("spring.datasource.username", dbUsername);
        }
        if (dbPassword != null && !dbPassword.isEmpty()) {
            secrets.put("spring.datasource.password", dbPassword);
        }
        
        System.out.println("=== SecretsPropertySourceFactory ===");
        System.out.println("DB_HOST: " + (dbHost != null ? "SET" : "NULL"));
        System.out.println("DB_USERNAME: " + (dbUsername != null ? "SET" : "NULL"));
        System.out.println("DB_PASSWORD: " + (dbPassword != null ? "SET" : "NULL"));
        System.out.println("Secrets loaded: " + secrets.size() + " properties");
        System.out.println("=====================================");
        
        return secrets;
    }

    private static class SecretsPropertySource extends PropertySource<Map<String, Object>> {
        SecretsPropertySource(String name, Map<String, Object> source) {
            super(name, source);
        }

        @Override
        @Nullable
        public Object getProperty(String name) {
            return this.source.get(name);
        }
    }
}
