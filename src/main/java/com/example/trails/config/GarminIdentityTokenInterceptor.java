package com.example.trails.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**** Adds a Cloud Run identity token for private Garmin service calls. */
public class GarminIdentityTokenInterceptor implements ClientHttpRequestInterceptor {

    private final IdTokenCredentials credentials;

    public GarminIdentityTokenInterceptor(String audience) throws IOException {
        GoogleCredentials applicationCredentials = GoogleCredentials.getApplicationDefault();
        this.credentials = IdTokenCredentials.newBuilder()
            .setIdTokenProvider((IdTokenProvider) applicationCredentials)
            .setTargetAudience(audience)
            .build();
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        String token = credentials.getRequestMetadata(request.getURI()).get("Authorization").get(0);
        request.getHeaders().setBearerAuth(token.substring("Bearer ".length()));
        return execution.execute(request, body);
    }
}