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
 * Load secrets from Google Cloud Secret Manager on application startup.
 */
@Component
public class SecretManagerInitializer implements ApplicationListener<ApplicationContextInitializedEvent> {

    @Override
    public void onApplicationEvent(ApplicationContextInitializedEvent event) {
        ConfigurableEnvironment env = event.getApplicationContext().getEnvironment();
        String activeProfile = env.getProperty("spring.profiles.active");
        
        if ("prod".equals(activeProfile)) {
            try {
                Map<String, Object> secrets = loadSecretsFromGCP();
                MapPropertySource propertySource = new MapPropertySource("gcpSecrets", secrets);
                env.getPropertySources().addFirst(propertySource);
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize secrets from Google Cloud Secret Manager", e);
            }
        }
    }

    private Map<String, Object> loadSecretsFromGCP() throws Exception {
        Map<String, Object> secrets = new HashMap<>();
        String projectId = getProjectId();
        
        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            secrets.put("DB_HOST", getSecret(client, projectId, "db-host"));
            secrets.put("DB_USERNAME", getSecret(client, projectId, "db-username"));
            secrets.put("DB_PASSWORD", getSecret(client, projectId, "db-password"));
        }
        return secrets;
    }

    private String getSecret(SecretManagerServiceClient client, String projectId, String secretId) {
        String secretName = String.format("projects/%s/secrets/%s/versions/latest", projectId, secretId);
        AccessSecretVersionRequest request = AccessSecretVersionRequest.newBuilder()
            .setName(secretName)
            .build();
        return client.accessSecretVersion(request).getPayload().getData().toStringUtf8();
    }

    private String getProjectId() {
        // Get GCP project ID from environment or metadata service
        String projectId = System.getenv("GOOGLE_CLOUD_PROJECT");
        if (projectId != null && !projectId.isEmpty()) {
            return projectId;
        }
        return "project-d1b0d97e-f7aa-4f2f-b78"; // Fallback
    }
}
