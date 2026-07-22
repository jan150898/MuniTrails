package com.example.trails.web;

import com.example.trails.model.GPXTrack;
import com.example.trails.model.GPXTrackStatus;
import com.example.trails.model.GPXTrackType;
import com.example.trails.model.User;
import com.example.trails.repo.GPXTrackRepository;
import com.example.trails.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GpxUploadController.class)
@WithMockUser(username = "testuser", roles = {"USER"})
@DisplayName("GpxUploadController Tests")
class GpxUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GPXTrackRepository gpxTrackRepository;

    @MockBean
    private UserService userService;

    private User testUser;
    private byte[] validGpxBytes;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");

        validGpxBytes = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<gpx version=\"1.1\" creator=\"Garmin\">\n" +
                "  <metadata><name>Test Track</name></metadata>\n" +
                "  <trk>\n" +
                "    <trkseg>\n" +
                "      <trkpt lat=\"47.5\" lon=\"11.5\"><ele>500</ele></trkpt>\n" +
                "      <trkpt lat=\"47.501\" lon=\"11.501\"><ele>550</ele></trkpt>\n" +
                "      <trkpt lat=\"47.502\" lon=\"11.502\"><ele>600</ele></trkpt>\n" +
                "    </trkseg>\n" +
                "  </trk>\n" +
                "</gpx>").getBytes(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Upload GPX file successfully")
    void testUploadGpxSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.gpx", "application/gpx+xml", validGpxBytes);

        GPXTrack savedTrack = new GPXTrack();
        savedTrack.setName("Test Track");
        savedTrack.setType(GPXTrackType.TOUR);
        savedTrack.setStatus(GPXTrackStatus.DRAFT);
        savedTrack.setVisibility(com.example.trails.model.Visibility.PUBLIC);



        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(gpxTrackRepository.save(any(GPXTrack.class))).thenReturn(savedTrack);

        mockMvc.perform(
                        multipart("/api/v1/tracks/upload-gpx")
                                .file(file)
                                .param("name", "Test Track")
                                .param("type", "TOUR")
                                .param("description", "Test Description")
                                .with(SecurityMockMvcRequestPostProcessors.csrf())
                                .principal(() -> "testuser")
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Track"))
                .andExpect(jsonPath("$.type").value("TOUR"));

        verify(gpxTrackRepository, times(1)).save(any(GPXTrack.class));
    }

    @Test
    @DisplayName("Upload GPX with invalid file returns BAD_REQUEST")
    void testUploadGpxInvalidFile() throws Exception {
        byte[] invalidGpx = "This is not valid XML".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "invalid.gpx", "application/gpx+xml", invalidGpx);

        when(userService.getUserByUsername("testuser")).thenReturn(testUser);

        mockMvc.perform(
                        multipart("/api/v1/tracks/upload-gpx")
                                .file(file)
                                .param("name", "Invalid Track")
                                .param("type", "TOUR")
                                .with(SecurityMockMvcRequestPostProcessors.csrf())
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Upload GPX without name uses file metadata name")
    void testUploadGpxWithoutName() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "track.gpx", "application/gpx+xml", validGpxBytes);

        GPXTrack savedTrack = new GPXTrack();
        savedTrack.setName("Test Track");
        savedTrack.setType(GPXTrackType.TOUR);
        savedTrack.setStatus(GPXTrackStatus.DRAFT);
        savedTrack.setVisibility(com.example.trails.model.Visibility.PUBLIC);




        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(gpxTrackRepository.save(any(GPXTrack.class))).thenReturn(savedTrack);

        mockMvc.perform(
                        multipart("/api/v1/tracks/upload-gpx")
                                .file(file)
                                .param("name", "")
                                .param("type", "TOUR")

                                .with(SecurityMockMvcRequestPostProcessors.csrf())
                                .principal(() -> "testuser")
                )
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Analyze GPX returns sections and points")
    void testAnalyzeGpx() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "analyze.gpx", "application/gpx+xml", validGpxBytes);

        mockMvc.perform(
                        multipart("/api/v1/tracks/analyze-gpx")
                                .file(file)
                                .with(SecurityMockMvcRequestPostProcessors.csrf())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Track"))
                .andExpect(jsonPath("$.points").isArray())
                .andExpect(jsonPath("$.sections").isArray());
    }

    @Test
    @DisplayName("Analyze GPX with invalid file returns error")
    void testAnalyzeGpxInvalidFile() throws Exception {
        byte[] invalidGpx = "Not XML".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "invalid.gpx", "application/gpx+xml", invalidGpx);

        mockMvc.perform(
                        multipart("/api/v1/tracks/analyze-gpx")
                                .file(file)
                                .with(SecurityMockMvcRequestPostProcessors.csrf())
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
