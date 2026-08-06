package com.example.trails.config;

import com.google.cloud.secretmanager.v1.AccessSecretVersionRequest;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.SimplePropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Load database credentials from Google Cloud Secret Manager for production.
 * Enabled only when spring.profiles.active=prod
 */
@Configuration
@ConditionalOnProperty(name = "spring.profiles.active", havingValue = "prod")
public class SecretManagerConfig {

    public static Map<String, String> loadSecrets(String projectId) {
        Map<String, String> secrets = new HashMap<>();
        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            secrets.put("db.username", getSecret(client, projectId, "db-username"));
            secrets.put("db.password", getSecret(client, projectId, "db-password"));
            secrets.put("db.host", getSecret(client, projectId, "db-host"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load secrets from Secret Manager", e);
        }
        return secrets;
    }

    private static String getSecret(SecretManagerServiceClient client, String projectId, String secretId) {
        String secretName = String.format("projects/%s/secrets/%s/versions/latest", projectId, secretId);
        AccessSecretVersionRequest request = AccessSecretVersionRequest.newBuilder()
            .setName(secretName)
            .build();
        return client.accessSecretVersion(request).getPayload().getData().toStringUtf8();
    }
}
