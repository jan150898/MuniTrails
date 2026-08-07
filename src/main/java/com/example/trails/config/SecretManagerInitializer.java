package com.example.trails.config;

import com.google.cloud.secretmanager.v1.AccessSecretVersionRequest;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Load secrets from Google Cloud Secret Manager on application startup (Cloud Run).
 * Falls back to environment variables if Google Cloud is unavailable (Fly.io, local).
 */
@Component
public class SecretManagerInitializer implements ApplicationListener<ApplicationContextInitializedEvent> {

    @Override
    public void onApplicationEvent(ApplicationContextInitializedEvent event) {
        ConfigurableEnvironment env = event.getApplicationContext().getEnvironment();
        String activeProfile = env.getProperty("spring.profiles.active");
        
        if ("prod".equals(activeProfile)) {
            try {
                Map<String, Object> secrets = loadSecrets();
                MapPropertySource propertySource = new MapPropertySource("secretsSource", secrets);
                env.getPropertySources().addFirst(propertySource);
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize secrets", e);
            }
        }
    }

    private Map<String, Object> loadSecrets() throws Exception {
        Map<String, Object> secrets = new HashMap<>();
        
        // Try Google Cloud Secret Manager first (Cloud Run)
        try {
            String projectId = getProjectId();
            if (projectId != null && !projectId.isEmpty()) {
                secrets.put("DB_HOST", getSecretFromGCP(projectId, "db-host"));
                secrets.put("DB_USERNAME", getSecretFromGCP(projectId, "db-username"));
                secrets.put("DB_PASSWORD", getSecretFromGCP(projectId, "db-password"));
                return secrets;
            }
        } catch (Exception e) {
            // Fall through to environment variables
        }
        
        // Fall back to environment variables (Fly.io, local, etc.)
        secrets.put("DB_HOST", System.getenv("DB_HOST"));
        secrets.put("DB_USERNAME", System.getenv("DB_USERNAME"));
        secrets.put("DB_PASSWORD", System.getenv("DB_PASSWORD"));
        
        return secrets;
    }

    private String getSecretFromGCP(String projectId, String secretId) throws Exception {
        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            String secretName = String.format("projects/%s/secrets/%s/versions/latest", projectId, secretId);
            AccessSecretVersionRequest request = AccessSecretVersionRequest.newBuilder()
                .setName(secretName)
                .build();
            return client.accessSecretVersion(request).getPayload().getData().toStringUtf8();
        }
    }

    private String getProjectId() {
        String projectId = System.getenv("GOOGLE_CLOUD_PROJECT");
        if (projectId != null && !projectId.isEmpty()) {
            return projectId;
        }
        return System.getenv("GCP_PROJECT");
    }
}
