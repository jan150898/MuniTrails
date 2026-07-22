package com.example.trails.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * HTTP Client configuration with optimized timeouts for Garmin integration
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(30))    // 30 seconds - Garmin can be slow
                .setReadTimeout(Duration.ofSeconds(60))       // 60 seconds - GPX download may take time
                .requestFactory(this::clientHttpRequestFactory)
                .build();
    }

    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);     // 30 seconds connection timeout
        factory.setReadTimeout(60000);        // 60 seconds read timeout
        factory.setBufferRequestBody(true);
        return new BufferingClientHttpRequestFactory(factory);
    }
}
