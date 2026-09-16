package com.example.trails.web;

import com.example.trails.dto.RegistrationRequest;
import com.example.trails.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for AuthController registration and verification endpoints
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegistrationService registrationService;

    @Test
    void testShowRegistrationFormReturnsRegistrationPage() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("registrationRequest"));
    }

    @Test
    void testShowVerificationFormReturnsVerificationPage() throws Exception {
        mockMvc.perform(get("/auth/verify-email"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/verify-email"))
                .andExpect(model().attributeExists("verificationRequest"));
    }

    @Test
    void testRegistrationWithValidDataCallsService() throws Exception {
        mockMvc.perform(post("/auth/register")
                .param("username", "testuser")
                .param("email", "test@example.com")
                .param("password", "securepass123")
                .param("passwordConfirm", "securepass123")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/verify-email"));

        ArgumentCaptor<RegistrationRequest> captor = ArgumentCaptor.forClass(RegistrationRequest.class);
        verify(registrationService).registerUser(captor.capture());

        RegistrationRequest captured = captor.getValue();
        assert captured.getUsername().equals("testuser");
        assert captured.getEmail().equals("test@example.com");
    }

    @Test
    void testRegistrationWithMissingUsernameReturnsError() throws Exception {
        mockMvc.perform(post("/auth/register")
                .param("username", "")
                .param("email", "test@example.com")
                .param("password", "securepass123")
                .param("passwordConfirm", "securepass123")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().hasErrors());
    }

    @Test
    void testRegistrationWithInvalidEmailReturnsError() throws Exception {
        mockMvc.perform(post("/auth/register")
                .param("username", "testuser")
                .param("email", "not-an-email")
                .param("password", "securepass123")
                .param("passwordConfirm", "securepass123")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().hasErrors());
    }

    @Test
    void testRegistrationWithPasswordTooShortReturnsError() throws Exception {
        mockMvc.perform(post("/auth/register")
                .param("username", "testuser")
                .param("email", "test@example.com")
                .param("password", "short")
                .param("passwordConfirm", "short")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().hasErrors());
    }

    @Test
    void testRegistrationWithMismatchedPasswordsReturnsError() throws Exception {
        mockMvc.perform(post("/auth/register")
                .param("username", "testuser")
                .param("email", "test@example.com")
                .param("password", "securepass123")
                .param("passwordConfirm", "differentpass123")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
    }

    @Test
    void testRegistrationWithDuplicateUsernameReturnsError() throws Exception {
        doThrow(new IllegalArgumentException("Username already exists"))
                .when(registrationService).registerUser(any());

        mockMvc.perform(post("/auth/register")
                .param("username", "existing")
                .param("email", "new@example.com")
                .param("password", "securepass123")
                .param("passwordConfirm", "securepass123")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrorCode("registrationRequest", "email", "error.registration"));
    }

    @Test
    void testRegistrationEndpointIsAccessibleWithoutAuthentication() throws Exception {
        // This test ensures the /auth/register endpoint doesn't require authentication
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
    }

    @Test
    void testVerificationEndpointIsAccessibleWithoutAuthentication() throws Exception {
        // This test ensures the /auth/verify-email endpoint doesn't require authentication
        mockMvc.perform(get("/auth/verify-email"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/verify-email"));
    }

    @Test
    void testVerificationFormPrefillsEmailFromQueryParameter() throws Exception {
        mockMvc.perform(get("/auth/verify-email").param("email", "prefill@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("prefill@example.com")));
    }

    @Test
    void testRegistrationCarriesEmailAndCredentialsToVerificationPage() throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/auth/register")
                .param("username", "flowuser")
                .param("email", "flow@example.com")
                .param("password", "securepass123")
                .param("passwordConfirm", "securepass123")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/verify-email"))
                .andExpect(flash().attribute("email", "flow@example.com"))
                .andExpect(flash().attribute("username", "flowuser"))
                .andReturn();

        // Follow the redirect within the same session so the flash attributes
        // are consumed, then verify the rendered form is prefilled.
        jakarta.servlet.http.Cookie sessionCookie =
                registerResult.getResponse().getCookie("SESSION");
        if (sessionCookie != null) {
            mockMvc.perform(get("/auth/verify-email").cookie(sessionCookie))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("flow@example.com")))
                    .andExpect(content().string(containsString("flowuser")));
        }
    }
}
