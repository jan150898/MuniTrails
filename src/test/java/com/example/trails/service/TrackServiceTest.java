package com.example.trails.service;

import com.example.trails.model.GPXTrack;
import com.example.trails.model.GPXTrackStatus;
import com.example.trails.model.GPXTrackType;
import com.example.trails.model.User;
import com.example.trails.model.Visibility;
import com.example.trails.repo.GPXTrackRepository;
import com.example.trails.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TrackService
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Track Service Tests")
class TrackServiceTest {

    @Autowired
    private TrackService trackService;

    @Autowired
    private GPXTrackRepository gpxTrackRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        gpxTrackRepository.deleteAll();
        userRepository.deleteAll();
        
        testUser = new User();
        testUser.setUsername("test_user");
        testUser.setPasswordHash(passwordEncoder.encode("password123"));
        testUser.setRole("ROLE_USER");
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("Create track successfully")
    void testCreateTrack() {
        GPXTrack track = trackService.createTrack(
                "Test Track",
                GPXTrackType.TOUR,
                47.5,
                11.5,
                10000,
                500,
                300,
                testUser
        );

        assertNotNull(track.getId(), "Track should have ID");
        assertEquals("Test Track", track.getName());
        assertEquals(GPXTrackType.TOUR, track.getType());
        assertEquals(47.5, track.getStartLat());
        assertEquals(11.5, track.getStartLon());
        assertEquals(10000, track.getDistanceMeters());
        assertEquals(500, track.getElevationGainMeters());
        assertEquals(300, track.getElevationLossMeters());
        assertEquals(testUser.getId(), track.getCreatedBy().getId());
        assertEquals(GPXTrackStatus.DRAFT, track.getStatus());
        assertEquals(Visibility.PUBLIC, track.getVisibility());
    }

    @Test
    @DisplayName("Find track by ID")
    void testFindById() {
        GPXTrack created = trackService.createTrack("Track1", GPXTrackType.TOUR, 47.5, 11.5, 1000, 100, 50, testUser);

        GPXTrack found = trackService.findById(created.getId());

        assertNotNull(found);
        assertEquals(created.getId(), found.getId());
        assertEquals("Track1", found.getName());
    }

    @Test
    @DisplayName("Find track by ID throws exception when not found")
    void testFindByIdNotFound() {
        assertThrows(RuntimeException.class, () -> {
            trackService.findById(java.util.UUID.randomUUID());
        });
    }

    @Test
    @DisplayName("Update track successfully")
    void testUpdateTrack() {
        GPXTrack created = trackService.createTrack("Original", GPXTrackType.TOUR, 47.5, 11.5, 1000, 100, 50, testUser);

        GPXTrack updated = trackService.updateTrack(
                created.getId(),
                "Updated Name",
                47.6,
                11.6,
                2000,
                200,
                100,
                testUser
        );

        assertEquals("Updated Name", updated.getName());
        assertEquals(47.6, updated.getStartLat());
        assertEquals(11.6, updated.getStartLon());
        assertEquals(2000, updated.getDistanceMeters());
        assertEquals(200, updated.getElevationGainMeters());
        assertEquals(100, updated.getElevationLossMeters());
    }

    @Test
    @DisplayName("Delete track")
    void testDeleteTrack() {
        GPXTrack created = trackService.createTrack("ToDelete", GPXTrackType.TOUR, 47.5, 11.5, 1000, 100, 50, testUser);

        trackService.deleteTrack(created.getId());

        assertThrows(RuntimeException.class, () -> {
            trackService.findById(created.getId());
        });
    }

    @Test
    @DisplayName("Find all tracks paginated")
    void testFindAllPaginated() {
        trackService.createTrack("Track1", GPXTrackType.TOUR, 47.5, 11.5, 1000, 100, 50, testUser);
        trackService.createTrack("Track2", GPXTrackType.TOUR, 47.6, 11.6, 2000, 200, 100, testUser);
        trackService.createTrack("Track3", GPXTrackType.UPHILL, 47.7, 11.7, 3000, 300, 150, testUser);

        Pageable pageable = PageRequest.of(0, 10);
        Page<GPXTrack> page = trackService.findAllPaginated(pageable);

        assertTrue(page.getTotalElements() >= 3);
    }

    @Test
    @DisplayName("Filter tracks by distance")
    void testFilterByDistance() {
        trackService.createTrack("Short", GPXTrackType.TOUR, 47.5, 11.5, 5000, 100, 50, testUser);
        trackService.createTrack("Long", GPXTrackType.TOUR, 47.6, 11.6, 50000, 500, 400, testUser);

        List<GPXTrack> filtered = trackService.filterTracks(30000.0, 60000.0, null, null, null, null, null, null, null, null, null, null);

        assertEquals(1, filtered.size());
        assertEquals("Long", filtered.get(0).getName());
    }

    @Test
    @DisplayName("Filter tracks by type")
    void testFilterByType() {
        trackService.createTrack("Trail1", GPXTrackType.TOUR, 47.5, 11.5, 1000, 100, 50, testUser);
        trackService.createTrack("Tour1", GPXTrackType.TOUR, 47.6, 11.6, 2000, 200, 100, testUser);

        List<GPXTrack> filtered = trackService.filterTracks(null, null, "TOUR", null, null, null, null, null, null, null, null, null);

        assertTrue(filtered.stream().allMatch(t -> t.getType() == GPXTrackType.TOUR));
    }

    @Test
    @DisplayName("Find all tracks summaries")
    void testFindAllSummaries() {
        trackService.createTrack("Track1", GPXTrackType.TOUR, 47.5, 11.5, 1000, 100, 50, testUser);
        trackService.createTrack("Track2", GPXTrackType.TOUR, 47.6, 11.6, 2000, 200, 100, testUser);

        var summaries = trackService.findAllSummaries();

        assertTrue(summaries.size() >= 2);
    }
}
