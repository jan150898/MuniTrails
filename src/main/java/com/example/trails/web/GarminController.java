package com.example.trails.web;

import com.example.trails.model.GPXTrack;
import com.example.trails.model.GPXTrackStatus;
import com.example.trails.model.GPXTrackType;
import com.example.trails.model.User;
import com.example.trails.model.Visibility;
import com.example.trails.repo.GPXTrackRepository;
import com.example.trails.service.UserService;
import com.example.trails.dto.TrackResponse;
import com.example.trails.dto.UploadSectionRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.security.Principal;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/v1/garmin")
public class GarminController {

    private final RestTemplate rest;

    private final ObjectMapper mapper = new ObjectMapper();
    private final GPXTrackRepository trackRepo;
    private final UserService userService;

    @Value("${garmin.service.url:http://garmin-service:5000}")
    private String garminServiceUrl;

    public GarminController(GPXTrackRepository trackRepo, UserService userService, RestTemplate restTemplate) {
        this.trackRepo = trackRepo;
        this.userService = userService;
        this.rest = restTemplate;
    }


    // ------------------------------------------------------------------
    // Login → returns { token }
    // ------------------------------------------------------------------
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Map<String, String> body) {
        try {
            ResponseEntity<String> resp = rest.postForEntity(
                garminServiceUrl + "/login",
                body,
                String.class
            );
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(502).body("{\"error\":\"Garmin service unavailable\"}");
        }
    }

    // ------------------------------------------------------------------
    // Logout
    // ------------------------------------------------------------------
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody Map<String, String> body) {
        try {
            ResponseEntity<String> resp = rest.postForEntity(
                garminServiceUrl + "/logout",
                body,
                String.class
            );
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (Exception e) {
            return ResponseEntity.ok("{\"status\":\"ok\"}");
        }
    }

    // ------------------------------------------------------------------
    // List activities
    // ------------------------------------------------------------------
    @GetMapping("/activities")
    public ResponseEntity<String> activities(
            @RequestParam String token,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        try {
            String url = garminServiceUrl + "/activities?token=" + token
                       + "&limit=" + limit + "&offset=" + offset;
            ResponseEntity<String> resp = rest.getForEntity(url, String.class);
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(502).body("{\"error\":\"Garmin service unavailable\"}");
        }
    }

    // ------------------------------------------------------------------
    // Import a Garmin activity as a Muni Trails tour
    // Fetches GPX from the Python service, then reuses the existing
    // GpxUploadController parsing logic via the track repo directly.
    // ------------------------------------------------------------------
    @PostMapping("/analyze/{activityId}")
    public ResponseEntity<?> analyzeActivity(@PathVariable long activityId, @RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "token required"));
        try {
            ResponseEntity<byte[]> response = rest.getForEntity(
                    garminServiceUrl + "/activity/" + activityId + "/gpx?token=" + token, byte[].class);
            if (response.getStatusCode().value() == 401 || response.getStatusCode().value() == 403) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "invalid or expired session token", "retryable", true));
            }
            if (response.getBody() == null) return ResponseEntity.status(502).body(Map.of("error", "Failed to download GPX"));
            GpxUploadController.GpxData data = GpxUploadController.parseGpxBytes(response.getBody());
            return ResponseEntity.ok(Map.of("name", data.getName(), "points", GpxUploadController.mapPoints(data),
                    "sections", GpxUploadController.detectSections(data)));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "invalid or expired session token", "retryable", true));
            }
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", "Could not analyze Garmin activity"));
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "Could not analyze Garmin activity: " + e.getMessage()));
        }
    }

    @PostMapping("/import/{activityId}")
    public ResponseEntity<?> importActivity(
            @PathVariable long activityId,
            @RequestBody Map<String, Object> body,
            Principal principal) {

        String token  = (String) body.get("token");
        String type   = (String) body.get("type");   // TOUR / TRAIL / UPHILL / DOWNHILL
        String name   = (String) body.get("name");   // optional override

        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "token required"));
        }
        if (type == null || type.isBlank()) {
            type = "TOUR";
        }

        // 1. Fetch GPX bytes from Python service (with token validation)
        byte[] gpxBytes;
        try {
            String url = garminServiceUrl + "/activity/" + activityId + "/gpx?token=" + token;
            ResponseEntity<byte[]> gpxResp = rest.getForEntity(url, byte[].class);
            
            // Check for token expiration or invalidity
            if (gpxResp.getStatusCode().value() == 401 || gpxResp.getStatusCode().value() == 403) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "invalid or expired session token", "retryable", true));
            }
            
            if (!gpxResp.getStatusCode().is2xxSuccessful() || gpxResp.getBody() == null) {
                return ResponseEntity.status(502).body(Map.of("error", "Failed to download GPX"));
            }
            gpxBytes = gpxResp.getBody();
        } catch (HttpClientErrorException e) {
            // Parse error response for session token issues
            String errorBody = e.getResponseBodyAsString();
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403 || 
                errorBody.contains("invalid or expired session token")) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "invalid or expired session token", "retryable", true));
            }
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", errorBody));
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "Garmin service unavailable: " + e.getMessage()));
        }

        // 2. Parse GPX using inline logic (mirrors GpxUploadController)
        GpxUploadController.GpxData gpxData;
        try {
            gpxData = GpxUploadController.parseGpxBytes(gpxBytes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "GPX parse error: " + e.getMessage()));
        }

        // 3. Persist the track
        try {
            User user = userService.getUserByUsername(principal.getName());
            GPXTrack track = new GPXTrack();
            track.setName(name != null && !name.isBlank() ? name : gpxData.getName());
            track.setType(GPXTrackType.valueOf(type.toUpperCase()));
            track.setStatus(GPXTrackStatus.DRAFT);
            track.setVisibility(Visibility.PUBLIC);
            track.setCreatedBy(user);
            track.setLastEditedBy(user);
            if (gpxData.getStartLat() != null) {
                track.setStartLat(gpxData.getStartLat());
                track.setStartLon(gpxData.getStartLon());
            }
            track.setDistanceMeters(gpxData.getDistanceMeters());
            track.setElevationGainMeters(gpxData.getElevationGainMeters());
            track.setElevationLossMeters(gpxData.getElevationLossMeters());
            track.setHighestPointAltitudeMeters(gpxData.getHighestPoint());
            track.setLowestPointAltitudeMeters(gpxData.getLowestPoint());
            track.setGpxFile(gpxBytes);
            track.setOverallRating(0);
            track.setExposition(5);
            track.setUphillRating(5);
            track.setRideAgain(false);
            GPXTrack saved = trackRepo.save(track);
            List<UploadSectionRequest> sections = body.containsKey("sections")
                    ? mapper.convertValue(body.get("sections"), new TypeReference<List<UploadSectionRequest>>() {})
                    : List.of();
            for (UploadSectionRequest section : sections) {
                if (!section.isIncluded() || section.getStartIndex() < 0 || section.getEndIndex() < section.getStartIndex()) continue;
                String sectionType = section.getType();
                if (!"UPHILL".equalsIgnoreCase(sectionType) && !"DOWNHILL".equalsIgnoreCase(sectionType)) continue;
                GpxUploadController.GpxData sectionData = gpxData.slice(section.getStartIndex(), section.getEndIndex());
                if (sectionData.getPoints().size() < 2) continue;
                GPXTrack extracted = new GPXTrack();
                extracted.setName(section.getName() == null || section.getName().isBlank()
                        ? saved.getName() + " – " + sectionType.toLowerCase() : section.getName());
                extracted.setType(GPXTrackType.valueOf(sectionType.toUpperCase()));
                extracted.setStatus(GPXTrackStatus.DRAFT);
                extracted.setVisibility(Visibility.PUBLIC);
                extracted.setCreatedBy(user);
                extracted.setLastEditedBy(user);
                extracted.setStartLat(sectionData.getStartLat());
                extracted.setStartLon(sectionData.getStartLon());
                extracted.setDistanceMeters(sectionData.getDistanceMeters());
                extracted.setElevationGainMeters(sectionData.getElevationGainMeters());
                extracted.setElevationLossMeters(sectionData.getElevationLossMeters());
                extracted.setHighestPointAltitudeMeters(sectionData.getHighestPoint());
                extracted.setLowestPointAltitudeMeters(sectionData.getLowestPoint());
                extracted.setGpxFile(sectionData.toGpxBytes());
                extracted.setOverallRating(section.getOverallRating());
                extracted.setExposition(section.getExposition());
                extracted.setUphillRating(section.getUphillRating());
                extracted.setRideAgain(section.isRideAgain());
                trackRepo.save(extracted);
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(new TrackResponse(saved));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to save track: " + e.getMessage()));
        }
    }
}
