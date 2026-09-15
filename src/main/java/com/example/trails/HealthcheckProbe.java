package com.example.trails;

import java.net.HttpURLConnection;
import java.net.URI;

/** Minimal JDK-only probe for the runtime image healthcheck. */
public final class HealthcheckProbe {
    private HealthcheckProbe() {
    }

    public static void main(String[] args) throws Exception {
        URI endpoint = URI.create(args.length == 0 ? "http://localhost:8080/actuator/health" : args[0]);
        HttpURLConnection connection = (HttpURLConnection) endpoint.toURL().openConnection();
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);
        connection.setRequestMethod("GET");
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("Health endpoint returned HTTP " + status);
        }
    }
}
