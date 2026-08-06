package com.example.trails.config;

import com.google.cloud.secretmanager.v1.AccessSecretVersionRequest;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;

/**
 * Utility to load database credentials from Google Cloud Secret Manager for production.
 */
public class SecretManagerConfig {

    public static String getSecret(String projectId, String secretId) throws Exception {
        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            String secretName = String.format("projects/%s/secrets/%s/versions/latest", projectId, secretId);
            AccessSecretVersionRequest request = AccessSecretVersionRequest.newBuilder()
                .setName(secretName)
                .build();
            return client.accessSecretVersion(request).getPayload().getData().toStringUtf8();
        }
    }
}
