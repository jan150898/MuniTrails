package com.example.trails;

import java.net.HttpURLConnection;
import java.net.URI;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/** Minimal JDK-only probe for the runtime image healthcheck. */
public final class HealthcheckProbe {
    private HealthcheckProbe() {
    }

    public static void main(String[] args) throws Exception {
        URI endpoint = URI.create(args.length == 0 ? "http://localhost:8080/actuator/health" : args[0]);
        if (endpoint.getScheme().equalsIgnoreCase("https")) {
            configureLocalHttpsProbe();
        }
        HttpURLConnection connection = (HttpURLConnection) endpoint.toURL().openConnection();
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);
        connection.setRequestMethod("GET");
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("Health endpoint returned HTTP " + status);
        }
    }

    private static void configureLocalHttpsProbe() throws Exception {
        TrustManager[] trustAll = {new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            public void checkClientTrusted(X509Certificate[] chain, String authType) { }
            public void checkServerTrusted(X509Certificate[] chain, String authType) { }
        }};
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, trustAll, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        HostnameVerifier localOnly = (hostname, session) -> hostname.equals("localhost") || hostname.equals("127.0.0.1");
        HttpsURLConnection.setDefaultHostnameVerifier(localOnly);
    }
}
