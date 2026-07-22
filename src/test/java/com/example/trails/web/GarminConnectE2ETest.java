package com.example.trails.web;

import com.example.trails.model.GPXTrack;
import com.example.trails.model.User;
import com.example.trails.repo.GPXTrackRepository;
import com.example.trails.repo.UserRepository;
import com.example.trails.service.GarminCredentialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Garmin Connect Integration E2E Tests
 * Tests the complete workflow: login → list activities → analyze → import
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Garmin Connect E2E Tests")
@Transactional
class GarminConnectE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GPXTrackRepository gpxTrackRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private GarminCredentialService garminCredentialService;

    @MockBean
    private RestTemplate restTemplate;

    private User testUser;
    private String mockGarminToken = "mock_garmin_token_12345";
    private byte[] mockGarminGpxBytes;

    @BeforeEach
    void setUp() {
        gpxTrackRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("garmin_test_user");
        testUser.setPasswordHash(passwordEncoder.encode("password123"));
        testUser.setRole("ROLE_USER");
        testUser = userRepository.save(testUser);

        // Mock GPX data from Garmin
        mockGarminGpxBytes = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<gpx version=\"1.1\" creator=\"Garmin\">\n" +
                "  <metadata><name>Garmin Activity - Alpenrose Loop</name></metadata>\n" +
                "  <trk>\n" +
                "    <trkseg>\n" +
                "      <trkpt lat=\"47.2\" lon=\"11.6\"><ele>800</ele></trkpt>\n" +
                "      <trkpt lat=\"47.205\" lon=\"11.605\"><ele>820</ele></trkpt>\n" +
                "      <trkpt lat=\"47.21\" lon=\"11.61\"><ele>840</ele></trkpt>\n" +
                "      <trkpt lat=\"47.215\" lon=\"11.615\"><ele>860</ele></trkpt>\n" +
                "      <trkpt lat=\"47.22\" lon=\"11.62\"><ele>880</ele></trkpt>\n" +
                "      <trkpt lat=\"47.225\" lon=\"11.625\"><ele>880</ele></trkpt>\n" +
                "      <trkpt lat=\"47.23\" lon=\"11.63\"><ele>860</ele></trkpt>\n" +
                "      <trkpt lat=\"47.235\" lon=\"11.635\"><ele>840</ele></trkpt>\n" +
                "      <trkpt lat=\"47.24\" lon=\"11.64\"><ele>820</ele></trkpt>\n" +
                "      <trkpt lat=\"47.245\" lon=\"11.645\"><ele>800</ele></trkpt>\n" +
                "    </trkseg>\n" +
                "  </trk>\n" +
                "</gpx>").getBytes(StandardCharsets.UTF_8);
    }

    @Test
    @WithMockUser(username = "garmin_test_user")
    @DisplayName("E2E: Save Garmin credentials securely")
    void testSaveGarminCredentialsE2E() throws Exception {
        String credentialBody = "{\"email\":\"user@garmin.com\",\"password\":\"garminpass123\"}";

        mockMvc.perform(post("/api/v1/profile/garmin/save")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentialBody)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Credentials saved securely."));

        // Verify credentials are encrypted in database
        var savedCreds = garminCredentialService.getCredentials(testUser);
        assertTrue(savedCreds.isPresent(), "Credentials should be saved");
        assertEquals("user@garmin.com", savedCreds.get().getEmail(), "Email should be decrypted correctly");
    }

    @Test
    @WithMockUser(username = "garmin_test_user")
    @DisplayName("E2E: Check if credentials exist (masked email)")
    void testCheckCredentialsExistE2E() throws Exception {
        // First save credentials
        garminCredentialService.saveCredentials(testUser, "user@garmin.com", "password");

        // Check if they exist
        mockMvc.perform(get("/api/v1/profile/garmin/has-credentials")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasCredentials").value(true))
                .andExpect(jsonPath("$.email").value(containsString("***"))); // Should be masked
    }

    @Test
    @WithMockUser(username = "garmin_test_user")
    @DisplayName("E2E: Delete Garmin credentials")
    void testDeleteGarminCredentialsE2E() throws Exception {
        // Save credentials first
        garminCredentialService.saveCredentials(testUser, "user@garmin.com", "password");
        assertTrue(garminCredentialService.hasCredentials(testUser), "Credentials should exist before delete");

        // Delete them
        mockMvc.perform(delete("/api/v1/profile/garmin/delete")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Credentials deleted."));

        // Verify they're gone
        assertFalse(garminCredentialService.hasCredentials(testUser), "Credentials should be deleted");
    }

    @Test
    @WithMockUser(username = "garmin_test_user")
    @DisplayName("E2E: Garmin login and get token")
    void testGarminLoginE2E() throws Exception {
        String loginBody = "{\"email\":\"user@example.com\",\"password\":\"password123\"}";

        // Mock the Garmin service response
        when(restTemplate.postForEntity(
                contains("/login"),
                any(),
                eq(String.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(
                        "{\"token\":\"" + mockGarminToken + "\"}",
                        org.springframework.http.HttpStatus.OK));

        MvcResult result = mockMvc.perform(post("/api/v1/garmin/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody)
                .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains(mockGarminToken), "Response should contain token");
    }

    @Test
    @WithMockUser(username = "garmin_test_user")
    @DisplayName("E2E: List Garmin activities")
    void testListGarminActivitiesE2E() throws Exception {
        String activitiesResponse = "[" +
                "{\"id\":123456,\"name\":\"Morning Ride\",\"type\":\"cycling\",\"date\":\"2024-01-15\"," +
                "\"distanceM\":25000,\"durationSecs\":5400,\"elevGainM\":450,\"isCycling\":true}," +
                "{\"id\":123457,\"name\":\"Afternoon Hike\",\"type\":\"hiking\",\"date\":\"2024-01-15\"," +
                "\"distanceM\":15000,\"durationSecs\":7200,\"elevGainM\":300,\"isCycling\":false}" +
                "]";

        when(restTemplate.getForEntity(
                contains("/activities"),
                eq(String.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(
                        activitiesResponse,
                        org.springframework.http.HttpStatus.OK));

        MvcResult result = mockMvc.perform(get("/api/v1/garmin/activities")
                .param("token", mockGarminToken)
                .param("limit", "20")
                .param("offset", "0")
                .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("Morning Ride"), "Should list activities");
        assertTrue(response.contains("cycling"), "Should show activity type");
    }

    @Test
    @WithMockUser(username = "garmin_test_user")
    @DisplayName("E2E: Analyze Garmin activity (parse GPX)")
    void testAnalyzeGarminActivityE2E() throws Exception {
        long activityId = 123456L;

        when(restTemplate.getForEntity(
                contains("/activity/" + activityId + "/gpx"),
                eq(byte[].class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(
                        mockGarminGpxBytes,
                        org.springframework.http.HttpStatus.OK));

        String analyzeBody = "{\"token\":\"" + mockGarminToken + "\"}";

        MvcResult result = mockMvc.perform(post("/api/v1/garmin/analyze/" + activityId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyzeBody)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Garmin Activity - Alpenrose Loop"))
                .andExpect(jsonPath("$.points").isArray())
                .andExpect(jsonPath("$.sections").isArray())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("points"), "Should contain GPS points");
        assertTrue(response.contains("sections"), "Should detect sections");
    }

    @Test
    @WithMockUser(username = "garmin_test_user")
    @DisplayName("E2E: Full Garmin import workflow")
    void testFullGarminImportWorkflowE2E() throws Exception {
        long activityId = 123456L;

        // Mock Garmin service to return GPX
        when(restTemplate.getForEntity(
                contains("/activity/" + activityId + "/gpx"),
                eq(byte[].class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(
                        mockGarminGpxBytes,
                        org.springframework.http.HttpStatus.OK));

        // Step 1: Import the activity
        String importBody = "{" +
                "\"token\":\"" + mockGarminToken + "\"," +
                "\"type\":\"TOUR\"," +
                "\"name\":\"My Garmin Tour\"," +
                "\"sections\":[" +
                "{\"startIndex\":0,\"endIndex\":4,\"type\":\"UPHILL\",\"name\":\"Climb\"," +
                "\"included\":true,\"overallRating\":8,\"exposition\":6,\"uphillRating\":8,\"rideAgain\":true}" +
                "]" +
                "}";

        MvcResult importResult = mockMvc.perform(post("/api/v1/garmin/import/" + activityId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(importBody)
                .with(csrf()))
                .andExpect(status().isCreated())
                .andReturn();

        String importResponse = importResult.getResponse().getContentAsString();
        assertTrue(importResponse.contains("My Garmin Tour"), "Should import tour with custom name");

        // Step 2: Verify tour in database
        GPXTrack importedTrack = gpxTrackRepository.findAll().stream()
                .filter(t -> "My Garmin Tour".equals(t.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Imported track should be saved"));

        assertEquals("My Garmin Tour", importedTrack.getName(), "Name should match");
        assertNotNull(importedTrack.getGpxFile(), "GPX file should be stored");
        assertTrue(importedTrack.getDistanceMeters() > 0, "Distance should be calculated");
        assertTrue(importedTrack.getElevationGainMeters() > 0, "Elevation should be calculated");
        assertEquals(testUser.getId(), importedTrack.getCreatedBy().getId(), "Creator should be current user");

        // Step 3: Verify tour appears on profile
        MvcResult profileResult = mockMvc.perform(get("/profile")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("My Garmin Tour")))
                .andReturn();

        String profileHtml = profileResult.getResponse().getContentAsString();
        assertTrue(profileHtml.contains("My Garmin Tour"), "Profile should display imported tour");
    }

    @Test
    @WithMockUser(username = "garmin_test_user")
    @DisplayName("E2E: Handle expired Garmin token")
    void testExpiredGarminTokenE2E() throws Exception {
        long activityId = 123456L;

        // Mock Garmin service returning 401 (unauthorized/expired token)
        when(restTemplate.getForEntity(
                contains("/activity/" + activityId + "/gpx"),
                eq(byte[].class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(
                        mockGarminGpxBytes,
                        org.springframework.http.HttpStatus.UNAUTHORIZED));

        String analyzeBody = "{\"token\":\"expired_token\"}";

        mockMvc.perform(post("/api/v1/garmin/analyze/" + activityId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyzeBody)
                .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value(containsString("invalid or expired session token")))
                .andExpect(jsonPath("$.retryable").value(true));
    }

    @Test
    @WithMockUser(username = "garmin_test_user")
    @DisplayName("E2E: Garmin logout")
    void testGarminLogoutE2E() throws Exception {
        String logoutBody = "{\"token\":\"" + mockGarminToken + "\"}";

        when(restTemplate.postForEntity(
                contains("/logout"),
                any(),
                eq(String.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(
                        "{\"status\":\"logged out\"}",
                        org.springframework.http.HttpStatus.OK));

        mockMvc.perform(post("/api/v1/garmin/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(logoutBody)
                .with(csrf()))
                .andExpect(status().isOk());
    }
}
