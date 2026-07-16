package com.example.trails.web;

import com.example.trails.model.GPXTrack;
import com.example.trails.model.GPXTrackStatus;
import com.example.trails.model.GPXTrackType;
import com.example.trails.model.User;
import com.example.trails.repo.GPXTrackRepository;
import com.example.trails.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GarminController.class)
@DisplayName("GarminController Tests")
class GarminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GPXTrackRepository gpxTrackRepository;

    @MockBean
    private UserService userService;

    @MockBean
    private RestTemplate restTemplate;

    private User testUser;
    private byte[] validGpxBytes;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");

        validGpxBytes = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<gpx version=\"1.1\" creator=\"Garmin\">\n" +
                "  <metadata><name>Garmin Activity</name></metadata>\n" +
                "  <trk>\n" +
                "    <trkseg>\n" +
                "      <trkpt lat=\"47.5\" lon=\"11.5\"><ele>500</ele></trkpt>\n" +
                "      <trkpt lat=\"47.501\" lon=\"11.501\"><ele>550</ele></trkpt>\n" +
                "    </trkseg>\n" +
                "  </trk>\n" +
                "</gpx>").getBytes(StandardCharsets.UTF_8);

        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Login endpoint forwards to Garmin service")
    void testLogin() throws Exception {
        String loginBody = "{\"email\":\"test@example.com\",\"password\":\"password\"}";

        mockMvc.perform(post("/api/v1/garmin/login")
                .contentType("application/json")
                .content(loginBody))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Import activity with valid token returns CREATED")
    void testImportActivitySuccess() throws Exception {
        GPXTrack savedTrack = new GPXTrack();
        savedTrack.setName("Garmin Activity");
        savedTrack.setType(GPXTrackType.TOUR);

        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(restTemplate.getForEntity(anyString(), eq(byte[].class)))
                .thenReturn(new ResponseEntity<>(validGpxBytes, HttpStatus.OK));
        when(gpxTrackRepository.save(any(GPXTrack.class))).thenReturn(savedTrack);

        String importBody = "{\"token\":\"valid_token_123\",\"type\":\"TOUR\",\"name\":\"My Activity\"}";

        mockMvc.perform(post("/api/v1/garmin/import/12345")
                .contentType("application/json")
                .content(importBody)
                .principal(() -> "testuser"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Garmin Activity"));
    }

    @Test
    @DisplayName("Import with expired token returns 401 with retryable flag")
    void testImportActivityExpiredToken() throws Exception {
        String errorMsg = "{\"error\":\"invalid or expired session token\"}";

        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(restTemplate.getForEntity(anyString(), eq(byte[].class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized", 
                        errorMsg.getBytes(), StandardCharsets.UTF_8));

        String importBody = "{\"token\":\"expired_token\",\"type\":\"TOUR\"}";

        mockMvc.perform(post("/api/v1/garmin/import/12345")
                .contentType("application/json")
                .content(importBody)
                .principal(() -> "testuser"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid or expired session token"))
                .andExpect(jsonPath("$.retryable").value(true));
    }

    @Test
    @DisplayName("Import with invalid token returns 401")
    void testImportActivityInvalidToken() throws Exception {
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(restTemplate.getForEntity(anyString(), eq(byte[].class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN, "Forbidden"));

        String importBody = "{\"token\":\"bad_token\",\"type\":\"TOUR\"}";

        mockMvc.perform(post("/api/v1/garmin/import/12345")
                .contentType("application/json")
                .content(importBody)
                .principal(() -> "testuser"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Import without token returns BAD_REQUEST")
    void testImportActivityNoToken() throws Exception {
        String importBody = "{\"type\":\"TOUR\"}";

        mockMvc.perform(post("/api/v1/garmin/import/12345")
                .contentType("application/json")
                .content(importBody)
                .principal(() -> "testuser"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("token required"));
    }

    @Test
    @DisplayName("Analyze activity returns sections and points")
    void testAnalyzeActivitySuccess() throws Exception {
        when(restTemplate.getForEntity(anyString(), eq(byte[].class)))
                .thenReturn(new ResponseEntity<>(validGpxBytes, HttpStatus.OK));

        String analyzeBody = "{\"token\":\"valid_token_123\"}";

        mockMvc.perform(post("/api/v1/garmin/analyze/12345")
                .contentType("application/json")
                .content(analyzeBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Garmin Activity"))
                .andExpect(jsonPath("$.points").isArray())
                .andExpect(jsonPath("$.sections").isArray());
    }

    @Test
    @DisplayName("Analyze activity without token returns BAD_REQUEST")
    void testAnalyzeActivityNoToken() throws Exception {
        String analyzeBody = "{}";

        mockMvc.perform(post("/api/v1/garmin/analyze/12345")
                .contentType("application/json")
                .content(analyzeBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("token required"));
    }

    @Test
    @DisplayName("Import with sections includes subsections")
    void testImportActivityWithSections() throws Exception {
        GPXTrack savedTrack = new GPXTrack();
        savedTrack.setName("Main Activity");
        savedTrack.setType(GPXTrackType.TOUR);

        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(restTemplate.getForEntity(anyString(), eq(byte[].class)))
                .thenReturn(new ResponseEntity<>(validGpxBytes, HttpStatus.OK));
        when(gpxTrackRepository.save(any(GPXTrack.class))).thenReturn(savedTrack);

        String importBody = "{\n" +
                "  \"token\":\"valid_token_123\",\n" +
                "  \"type\":\"TOUR\",\n" +
                "  \"name\":\"My Activity\",\n" +
                "  \"sections\":[\n" +
                "    {\"startIndex\":0,\"endIndex\":1,\"type\":\"UPHILL\",\"name\":\"Climb\",\"included\":true}\n" +
                "  ]\n" +
                "}";

        mockMvc.perform(post("/api/v1/garmin/import/12345")
                .contentType("application/json")
                .content(importBody)
                .principal(() -> "testuser"))
                .andExpect(status().isCreated());

        verify(gpxTrackRepository, atLeast(1)).save(any(GPXTrack.class));
    }

    @Test
    @DisplayName("Activities endpoint lists activities")
    void testActivitiesList() throws Exception {
        mockMvc.perform(get("/api/v1/garmin/activities")
                .param("token", "valid_token_123")
                .param("limit", "20")
                .param("offset", "0"))
                .andExpect(status().isOk());
    }

}
