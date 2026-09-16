package com.example.trails.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for SecurityConfig to ensure endpoints are properly secured/permitted
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testLoginPageIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void testRegistrationPageIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk());
    }

    @Test
    void testVerificationPageIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/auth/verify-email"))
                .andExpect(status().isOk());
    }

    @Test
    void testHealthCheckIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void testCssFilesAreAccessibleWithoutAuthentication() throws Exception {
        // Note: This will return 404 if file doesn't exist, but won't redirect to login
        mockMvc.perform(get("/css/test.css"))
                .andExpect(status().isNotFound()); // Not 302 redirect to login
    }

    @Test
    void testJsFilesAreAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/js/test.js"))
                .andExpect(status().isNotFound()); // Not 302 redirect to login
    }

    @Test
    void testStaticResourcesAreAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/images/test.png"))
                .andExpect(status().isNotFound()); // Not 302 redirect to login
    }

    @Test
    void testRootPathIsAccessibleWithoutAuthentication() throws Exception {
        // Root path should either be OK or redirect (not 403 Forbidden)
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    void testAuthEndpointsDoNotRedirectToLogin() throws Exception {
        // Verify /auth/** paths don't cause redirect loops
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/auth/verify-email"))
                .andExpect(status().isOk());

        // POST endpoints should also be permitted (returns 405 Method Not Allowed for GET, not 302)
        mockMvc.perform(get("/auth/resend-code"))
                .andExpect(status().isMethodNotAllowed()); // Not 302 redirect
    }
}
