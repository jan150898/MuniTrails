package com.example.trails.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * HTTP Client configuration with optimized timeouts for Garmin integration
 */
@Configuration
public class HttpClientConfig {

    @Value("${garmin.service.use-identity-token:false}")
    private boolean useIdentityToken;

    @Value("${garmin.service.audience:}")
    private String garminServiceAudience;

    @Bean
    public RestTemplate restTemplate() throws Exception {
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory());
        if (useIdentityToken) {
            if (garminServiceAudience.isBlank()) {
                throw new IllegalStateException("garmin.service.audience is required when identity-token authentication is enabled");
            }
            restTemplate.getInterceptors().add(
                    new GarminIdentityTokenInterceptor(garminServiceAudience));
        }
        return restTemplate;
    }

    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);     // 30 seconds connection timeout
        factory.setReadTimeout(60000);        // 60 seconds read timeout
        return factory;
    }
}
