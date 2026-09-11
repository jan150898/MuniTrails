package com.example.trails.web;

import com.example.trails.model.*;
import com.example.trails.repo.GPXTrackRepository;
import com.example.trails.repo.UserRepository;
import com.example.trails.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-End Integration Tests for TrailsSpring
 * Tests complete user workflows from login to GPX upload
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("TrailsSpring E2E Tests")
@Transactional
class TrailsSpringE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GPXTrackRepository gpxTrackRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private RestTemplate restTemplate;

    private User testUser;
    private byte[] validGpxBytes;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash(passwordEncoder.encode("TestPassword123"));
        testUser.setRole("ROLE_USER");
        userRepository.save(testUser);

        // Valid GPX data for testing
        validGpxBytes = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<gpx version=\"1.1\" creator=\"Test\">\n" +
                "  <metadata><name>Test Track</name></metadata>\n" +
                "  <trk>\n" +
                "    <trkseg>\n" +
                "      <trkpt lat=\"47.5\" lon=\"11.5\"><ele>500</ele></trkpt>\n" +
                "      <trkpt lat=\"47.501\" lon=\"11.501\"><ele>510</ele></trkpt>\n" +
                "      <trkpt lat=\"47.502\" lon=\"11.502\"><ele>520</ele></trkpt>\n" +
                "      <trkpt lat=\"47.503\" lon=\"11.503\"><ele>530</ele></trkpt>\n" +
                "      <trkpt lat=\"47.504\" lon=\"11.504\"><ele>540</ele></trkpt>\n" +
                "    </trkseg>\n" +
                "  </trk>\n" +
                "</gpx>").getBytes(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("E2E: User logs in, uploads GPX, and views track")
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void testCompleteGpxUploadWorkflow() throws Exception {
        // 1. Create GPX file
        MockMultipartFile gpxFile = new MockMultipartFile(
                "file", "test-track.gpx", "application/gpx+xml", validGpxBytes);

        // 2. Upload GPX
        mockMvc.perform(multipart("/api/v1/tracks/upload-gpx")
                .file(gpxFile)
                .param("name", "E2E Test Track")
                .param("type", "TOUR")
                .param("description", "Test track for E2E testing")
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("E2E Test Track"))
                .andExpect(jsonPath("$.type").value("TOUR"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        // 3. Verify track was saved to database
        var tracks = gpxTrackRepository.findAll();
        assertNotNull(tracks);
        assertTrue(tracks.size() > 0, "Track should be saved to database");

        GPXTrack savedTrack = tracks.get(0);
        assertEquals("E2E Test Track", savedTrack.getName());
        assertEquals(GPXTrackType.TOUR, savedTrack.getType());
        assertEquals(GPXTrackStatus.DRAFT, savedTrack.getStatus());
        assertNotNull(savedTrack.getGpxFile());
        assertTrue(savedTrack.getGpxFile().length > 0);
    }

    @Test
    @DisplayName("E2E: Analyze GPX without upload")
    void testAnalyzeGpxWithoutUpload() throws Exception {
        MockMultipartFile gpxFile = new MockMultipartFile(
                "file", "analyze.gpx", "application/gpx+xml", validGpxBytes);

        mockMvc.perform(multipart("/api/v1/tracks/analyze-gpx")
                .file(gpxFile)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.points").isArray())
                .andExpect(jsonPath("$.points.length()").greaterThan(0))
                .andExpect(jsonPath("$.sections").isArray());
    }

    @Test
    @DisplayName("E2E: Section detection on complex GPX")
    void testComplexGpxWithSectionDetection() throws Exception {
        // GPX with elevation changes (uphill then downhill)
        byte[] complexGpx = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<gpx version=\"1.1\" creator=\"Test\">\n" +
                "  <metadata><name>Complex Track</name></metadata>\n" +
                "  <trk>\n" +
                "    <trkseg>\n" +
                "      <trkpt lat=\"47.5\" lon=\"11.5\"><ele>0</ele></trkpt>\n" +
                "      <trkpt lat=\"47.501\" lon=\"11.501\"><ele>50</ele></trkpt>\n" +
                "      <trkpt lat=\"47.502\" lon=\"11.502\"><ele>100</ele></trkpt>\n" +
                "      <trkpt lat=\"47.503\" lon=\"11.503\"><ele>150</ele></trkpt>\n" +
                "      <trkpt lat=\"47.504\" lon=\"11.504\"><ele>200</ele></trkpt>\n" +
                "      <trkpt lat=\"47.505\" lon=\"11.505\"><ele>150</ele></trkpt>\n" +
                "      <trkpt lat=\"47.506\" lon=\"11.506\"><ele>100</ele></trkpt>\n" +
                "      <trkpt lat=\"47.507\" lon=\"11.507\"><ele>50</ele></trkpt>\n" +
                "      <trkpt lat=\"47.508\" lon=\"11.508\"><ele>0</ele></trkpt>\n" +
                "    </trkseg>\n" +
                "  </trk>\n" +
                "</gpx>").getBytes(StandardCharsets.UTF_8);

        MockMultipartFile gpxFile = new MockMultipartFile(
                "file", "complex.gpx", "application/gpx+xml", complexGpx);

        mockMvc.perform(multipart("/api/v1/tracks/analyze-gpx")
                .file(gpxFile)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections").isArray())
                .andExpect(jsonPath("$.sections.length()").greaterThan(0))
                .andExpect(jsonPath("$.sections[0].type").value(containsString("UPHILL")));
    }

    @Test
    @DisplayName("E2E: Multiple GPX uploads by same user")
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void testMultipleTracksUpload() throws Exception {
        // Upload first track
        MockMultipartFile file1 = new MockMultipartFile(
                "file", "track1.gpx", "application/gpx+xml", validGpxBytes);
        mockMvc.perform(multipart("/api/v1/tracks/upload-gpx")
                .file(file1)
                .param("name", "Track One")
                .param("type", "TOUR")
                .with(csrf()))
                .andExpect(status().isCreated());

        // Upload second track
        MockMultipartFile file2 = new MockMultipartFile(
                "file", "track2.gpx", "application/gpx+xml", validGpxBytes);
        mockMvc.perform(multipart("/api/v1/tracks/upload-gpx")
                .file(file2)
                .param("name", "Track Two")
                .param("type", "UPHILL")
                .with(csrf()))
                .andExpect(status().isCreated());

        // Verify both tracks exist
        var tracks = gpxTrackRepository.findAll();
        assertEquals(2, tracks.size(), "Both tracks should be in database");
    }

    @Test
    @DisplayName("E2E: Invalid GPX file is rejected")
    void testInvalidGpxRejection() throws Exception {
        byte[] invalidGpx = "This is not valid XML or GPX".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "invalid.gpx", "application/gpx+xml", invalidGpx);

        mockMvc.perform(multipart("/api/v1/tracks/upload-gpx")
                .file(file)
                .param("name", "Invalid")
                .param("type", "TOUR")
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("E2E: GPX with single point is rejected")
    void testSinglePointGpxRejection() throws Exception {
        byte[] singlePointGpx = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<gpx version=\"1.1\" creator=\"Test\">\n" +
                "  <trk>\n" +
                "    <trkseg>\n" +
                "      <trkpt lat=\"47.5\" lon=\"11.5\"><ele>500</ele></trkpt>\n" +
                "    </trkseg>\n" +
                "  </trk>\n" +
                "</gpx>").getBytes(StandardCharsets.UTF_8);

        MockMultipartFile file = new MockMultipartFile(
                "file", "single.gpx", "application/gpx+xml", singlePointGpx);

        mockMvc.perform(multipart("/api/v1/tracks/upload-gpx")
                .file(file)
                .param("name", "Single Point")
                .param("type", "TOUR")
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("E2E: Empty GPX file is rejected")
    void testEmptyGpxRejection() throws Exception {
        byte[] emptyGpx = "".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.gpx", "application/gpx+xml", emptyGpx);

        mockMvc.perform(multipart("/api/v1/tracks/upload-gpx")
                .file(file)
                .param("name", "Empty")
                .param("type", "TOUR")
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("E2E: Concurrent requests handling")
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void testConcurrentRequests() throws Exception {
        // Simulate multiple rapid requests (all should succeed or gracefully fail)
        for (int i = 0; i < 3; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "track" + i + ".gpx", "application/gpx+xml", validGpxBytes);
            mockMvc.perform(multipart("/api/v1/tracks/upload-gpx")
                    .file(file)
                    .param("name", "Concurrent Track " + i)
                    .param("type", "TOUR")
                    .with(csrf()))
                    .andExpect(status().isCreated());
        }

        var tracks = gpxTrackRepository.findAll();
        assertTrue(tracks.size() >= 3, "All concurrent uploads should succeed");
    }

    @Test
    @DisplayName("E2E: Unauthenticated access denied")
    void testUnauthenticatedAccessDenied() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.gpx", "application/gpx+xml", validGpxBytes);

        mockMvc.perform(multipart("/api/v1/tracks/upload-gpx")
                .file(file)
                .param("name", "Test")
                .param("type", "TOUR")
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("E2E: Large GPX file handling")
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void testLargeGpxFile() throws Exception {
        // Create GPX with many points (10KB+)
        StringBuilder largeGpx = new StringBuilder(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<gpx version=\"1.1\" creator=\"Test\">\n" +
                "  <trk>\n" +
                "    <trkseg>\n");

        double lat = 47.5;
        double lon = 11.5;
        for (int i = 0; i < 500; i++) {
            largeGpx.append(String.format(
                    "      <trkpt lat=\"%.3f\" lon=\"%.3f\"><ele>%d</ele></trkpt>\n",
                    lat + (i * 0.001), lon + (i * 0.001), 500 + i));
        }

        largeGpx.append("    </trkseg>\n")
                .append("  </trk>\n")
                .append("</gpx>");

        byte[] largeGpxBytes = largeGpx.toString().getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.gpx", "application/gpx+xml", largeGpxBytes);

        mockMvc.perform(multipart("/api/v1/tracks/upload-gpx")
                .file(file)
                .param("name", "Large Track")
                .param("type", "TOUR")
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Large Track"));
    }

    @Test
    @DisplayName("E2E: Health check endpoint working")
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
