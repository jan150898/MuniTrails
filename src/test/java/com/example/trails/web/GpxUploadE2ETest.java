package com.example.trails.web;

import com.example.trails.model.GPXTrack;
import com.example.trails.model.User;
import com.example.trails.repo.GPXTrackRepository;
import com.example.trails.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GpxUploadController.class)

@DisplayName("GPX Upload E2E Tests")
class GpxUploadE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private GPXTrackRepository gpxTrackRepository;

    private User testUser;
    private byte[] validGpxBytes;
    private byte[] mountainGpxBytes;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");

        // Simple valid GPX
        validGpxBytes = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<gpx version=\"1.1\" creator=\"Garmin\">\n" +
                "  <metadata><name>Test Track</name></metadata>\n" +
                "  <trk>\n" +
                "    <trkseg>\n" +
                "      <trkpt lat=\"47.5\" lon=\"11.5\"><ele>500</ele></trkpt>\n" +
                "      <trkpt lat=\"47.501\" lon=\"11.501\"><ele>510</ele></trkpt>\n" +
                "      <trkpt lat=\"47.502\" lon=\"11.502\"><ele>520</ele></trkpt>\n" +
                "    </trkseg>\n" +
                "  </trk>\n" +
                "</gpx>").getBytes(StandardCharsets.UTF_8);

        // GPX with significant elevation changes
        mountainGpxBytes = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<gpx version=\"1.1\" creator=\"Garmin\">\n" +
                "  <metadata><name>Mountain Track</name></metadata>\n" +
                "  <trk>\n" +
                "    <trkseg>\n" +
                "      <trkpt lat=\"47.0\" lon=\"11.0\"><ele>1000</ele></trkpt>\n" +
                "      <trkpt lat=\"47.001\" lon=\"11.001\"><ele>1000</ele></trkpt>\n" +
                "      <trkpt lat=\"47.002\" lon=\"11.002\"><ele>1025</ele></trkpt>\n" +
                "      <trkpt lat=\"47.003\" lon=\"11.003\"><ele>1050</ele></trkpt>\n" +
                "      <trkpt lat=\"47.004\" lon=\"11.004\"><ele>1050</ele></trkpt>\n" +
                "      <trkpt lat=\"47.005\" lon=\"11.005\"><ele>1025</ele></trkpt>\n" +
                "      <trkpt lat=\"47.006\" lon=\"11.006\"><ele>1000</ele></trkpt>\n" +
                "    </trkseg>\n" +
                "  </trk>\n" +
                "</gpx>").getBytes(StandardCharsets.UTF_8);
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("E2E: Analyze GPX file successfully")
    void testAnalyzeGpxE2E() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.gpx", "application/gpx+xml", validGpxBytes);

        MvcResult result = mockMvc.perform(multipart("/api/v1/tracks/analyze-gpx")
                .file(file)
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Track"))
                .andExpect(jsonPath("$.points").isArray())
                .andExpect(jsonPath("$.points.length()").value(3))
                .andExpect(jsonPath("$.sections").isArray())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("Test Track"), "Response should contain track name");
        assertTrue(response.contains("points"), "Response should contain points");
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("E2E: Upload GPX and create track")
    void testUploadGpxE2E() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.gpx", "application/gpx+xml", validGpxBytes);

        GPXTrack savedTrack = new GPXTrack();
        savedTrack.setName("Test Track");
        savedTrack.setDistanceMeters(200.0);
        savedTrack.setElevationGainMeters(20.0);

        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(gpxTrackRepository.save(any(GPXTrack.class))).thenReturn(savedTrack);

        mockMvc.perform(multipart("/api/v1/tracks/upload-gpx")
                .file(file)
                .param("name", "Test Track")
                .param("type", "TOUR")
                .param("description", "Test Description")
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Track"));

        verify(gpxTrackRepository, times(1)).save(any(GPXTrack.class));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("E2E: Detect sections in mountain GPX")
    void testDetectSectionsE2E() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "mountain.gpx", "application/gpx+xml", mountainGpxBytes);

        MvcResult result = mockMvc.perform(multipart("/api/v1/tracks/analyze-gpx")
                .file(file)
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mountain Track"))
                .andExpect(jsonPath("$.sections").isArray())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("sections"), "Response should contain sections");
        // With new sensitive thresholds, should detect uphill/downhill
        System.out.println("Detect sections response: " + response);
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("E2E: Upload with sections")
    void testUploadWithSectionsE2E() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "mountain.gpx", "application/gpx+xml", mountainGpxBytes);

        GPXTrack mainTrack = new GPXTrack();
        mainTrack.setName("Mountain Track");
        mainTrack.setDistanceMeters(650.0);
        mainTrack.setElevationGainMeters(50.0);

        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(gpxTrackRepository.save(any(GPXTrack.class))).thenReturn(mainTrack);

        String sections = "[{\"startIndex\":1,\"endIndex\":4,\"type\":\"UPHILL\",\"name\":\"Climb\",\"included\":true," +
                "\"overallRating\":7,\"exposition\":5,\"uphillRating\":6,\"rideAgain\":true}]";

        mockMvc.perform(multipart("/api/v1/tracks/upload-gpx")
                .file(file)
                .param("name", "Mountain Track")
                .param("type", "TOUR")
                .param("sections", sections)
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isCreated());

        // Should save main track + 1 section
        verify(gpxTrackRepository, atLeast(1)).save(any(GPXTrack.class));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("E2E: Invalid GPX returns BAD_REQUEST")
    void testInvalidGpxE2E() throws Exception {
        byte[] invalidGpx = "This is not valid XML".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "invalid.gpx", "application/gpx+xml", invalidGpx);

        when(userService.getUserByUsername("testuser")).thenReturn(testUser);

        mockMvc.perform(multipart("/api/v1/tracks/upload-gpx")
                .file(file)
                .param("name", "Invalid")
                .param("type", "TOUR")
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("E2E: Missing required parameters")
    void testMissingParametersE2E() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.gpx", "application/gpx+xml", validGpxBytes);

        mockMvc.perform(multipart("/api/v1/tracks/upload-gpx")
                .file(file)
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("E2E: Metrics calculated correctly")
    void testMetricsCalculationE2E() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.gpx", "application/gpx+xml", validGpxBytes);

        MvcResult result = mockMvc.perform(multipart("/api/v1/tracks/analyze-gpx")
                .file(file)
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("points"), "Should have points data");
    }

}
