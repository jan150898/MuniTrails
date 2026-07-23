package com.example.trails.web;

import com.example.trails.model.GarminActivityCache;
import com.example.trails.model.GPXTrack;
import com.example.trails.model.GPXTrackStatus;
import com.example.trails.model.GPXTrackType;
import com.example.trails.model.User;
import com.example.trails.model.Visibility;
import com.example.trails.repo.GarminActivityCacheRepository;
import com.example.trails.repo.GPXTrackRepository;
import com.example.trails.service.UserService;
import com.example.trails.dto.TrackResponse;
import com.example.trails.dto.UploadSectionRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(GarminController.class);

    private final RestTemplate rest;
    private final ObjectMapper mapper = new ObjectMapper();
    private final GPXTrackRepository trackRepo;
    private final UserService userService;
    private final GarminActivityCacheRepository cacheRepo;

    @Value("${garmin.service.url:http://garmin-service:5000}")
    private String garminServiceUrl;

    public GarminController(GPXTrackRepository trackRepo, UserService userService, RestTemplate restTemplate,
                          GarminActivityCacheRepository cacheRepo) {
        this.trackRepo = trackRepo;
        this.userService = userService;
        this.rest = restTemplate;
        this.cacheRepo = cacheRepo;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Map<String, String> body) {
        try {
            logger.info("Attempting Garmin login");
            long startTime = System.currentTimeMillis();
            
            ResponseEntity<String> resp = rest.postForEntity(
                garminServiceUrl + "/login",
                body,
                String.class
            );
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Garmin login completed in {} ms", duration);
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (HttpClientErrorException e) {
            logger.warn("Garmin login failed: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Garmin service unavailable", e);
            return ResponseEntity.status(502).body("{\"error\":\"Garmin service unavailable\"}");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody Map<String, String> body) {
        try {
            logger.info("Garmin logout");
            ResponseEntity<String> resp = rest.postForEntity(
                garminServiceUrl + "/logout",
                body,
                String.class
            );
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (Exception e) {
            logger.warn("Logout failed", e);
            return ResponseEntity.ok("{\"status\":\"ok\"}");
        }
    }

    @GetMapping("/activities")
    public ResponseEntity<String> activities(
            @RequestParam String token,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        try {
            logger.info("Fetching Garmin activities: limit={}, offset={}", limit, offset);
            long startTime = System.currentTimeMillis();
            
            String url = garminServiceUrl + "/activities?token=" + token
                       + "&limit=" + limit + "&offset=" + offset;
            ResponseEntity<String> resp = rest.getForEntity(url, String.class);
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Activities fetched in {} ms", duration);
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (HttpClientErrorException e) {
            logger.warn("Failed to fetch activities: {}", e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Garmin service unavailable", e);
            return ResponseEntity.status(502).body("{\"error\":\"Garmin service unavailable\"}");
        }
    }

    @PostMapping("/analyze/{activityId}")
    public ResponseEntity<?> analyzeActivity(@PathVariable long activityId, @RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "token required"));
        
        try {
            logger.info("Analyzing Garmin activity: {}", activityId);
            long startTime = System.currentTimeMillis();
            
            ResponseEntity<byte[]> response = rest.getForEntity(
                    garminServiceUrl + "/activity/" + activityId + "/gpx?token=" + token, byte[].class);
            
            if (response.getStatusCode().value() == 401 || response.getStatusCode().value() == 403) {
                logger.warn("Token expired or invalid for activity {}", activityId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "invalid or expired session token", "retryable", true));
            }
            if (response.getBody() == null) {
                logger.error("Empty GPX response for activity {}", activityId);
                return ResponseEntity.status(502).body(Map.of("error", "Failed to download GPX"));
            }
            
            logger.debug("Parsing GPX data ({} bytes)", response.getBody().length);
            GpxUploadController.GpxData data = GpxUploadController.parseGpxBytes(response.getBody());
            
            if (data.getPoints().isEmpty()) {
                logger.error("Activity {} returned empty GPX", activityId);
                return ResponseEntity.status(502).body(Map.of("error", "Activity has no track points"));
            }
            
            logger.debug("GPX parsed successfully: {} points, startLat={}, startLon={}", 
                data.getPoints().size(), data.getStartLat(), data.getStartLon());
            
            var sections = SectionDetector.detectSections(data.getPoints());
            var pointsMapped = GpxUploadController.mapPoints(data);
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Activity {} analyzed in {} ms: {} points, {} sections", 
                activityId, duration, pointsMapped.size(), sections.size());
            
            return ResponseEntity.ok(Map.of("name", data.getName(), "points", pointsMapped,
                    "sections", sections));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                logger.warn("Unauthorized access to activity {}", activityId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "invalid or expired session token", "retryable", true));
            }
            logger.error("HTTP error analyzing activity {}: {}", activityId, e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", "Could not analyze Garmin activity"));
        } catch (Exception e) {
            logger.error("Error analyzing activity " + activityId, e);
            return ResponseEntity.status(502).body(Map.of("error", "Could not analyze Garmin activity: " + e.getMessage()));
        }
    }

    @PostMapping("/cache-activities")
    public ResponseEntity<?> cacheActivities(
            @RequestBody Map<String, String> body,
            Principal principal) {
        String token = body.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "token required"));
        }
        
        try {
            logger.info("Caching Garmin activities");
            long startTime = System.currentTimeMillis();
            
            String url = garminServiceUrl + "/activities?token=" + token + "&limit=100&offset=0";
            ResponseEntity<String> resp = rest.getForEntity(url, String.class);
            
            if (!resp.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
            }
            
            JsonNode activitiesNode = mapper.readTree(resp.getBody());
            User user = userService.getUserByUsername(principal.getName());
            
            GarminActivityCache cache = cacheRepo.findByUser(user)
                    .orElseGet(() -> new GarminActivityCache());
            cache.setUser(user);
            cache.setActivities(activitiesNode);
            cacheRepo.save(cache);
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Activities cached in {} ms", duration);
            
            return ResponseEntity.ok(Map.of(
                "cached", true,
                "count", activitiesNode.size(),
                "duration", duration
            ));
        } catch (Exception e) {
            logger.error("Error caching activities", e);
            return ResponseEntity.status(502).body(Map.of("error", "Failed to cache activities: " + e.getMessage()));
        }
    }

    @GetMapping("/cached-activities")
    public ResponseEntity<?> getCachedActivities(Principal principal) {
        try {
            User user = userService.getUserByUsername(principal.getName());
            var cache = cacheRepo.findByUser(user);
            
            if (cache.isEmpty() || cache.get().isExpired()) {
                return ResponseEntity.ok(Map.of(
                    "cached", false,
                    "message", "No valid cached activities"
                ));
            }
            
            JsonNode activities = cache.get().getActivities();
            long cacheAgeSeconds = java.time.Duration.between(
                cache.get().getCachedAt(),
                java.time.Instant.now()
            ).getSeconds();
            
            logger.info("Returning {} cached activities", activities.size());
            
            return ResponseEntity.ok(Map.of(
                "cached", true,
                "activities", activities,
                "cacheAgeSeconds", cacheAgeSeconds
            ));
        } catch (Exception e) {
            logger.error("Error retrieving cached activities", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to retrieve cache"));
        }
    }

    @PostMapping("/import/{activityId}")
    public ResponseEntity<?> importActivity(
            @PathVariable long activityId,
            @RequestBody Map<String, Object> body,
            Principal principal) {

        String token  = (String) body.get("token");
        String type   = (String) body.get("type");
        String name   = (String) body.get("name");

        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "token required"));
        }
        if (type == null || type.isBlank()) {
            type = "TOUR";
        }

        logger.info("Importing Garmin activity {} as {}", activityId, type);
        long totalStart = System.currentTimeMillis();

        // 1. Fetch GPX bytes from Garmin service
        byte[] gpxBytes;
        try {
            logger.debug("Downloading GPX for activity {}", activityId);
            long start = System.currentTimeMillis();
            
            String url = garminServiceUrl + "/activity/" + activityId + "/gpx?token=" + token;
            ResponseEntity<byte[]> gpxResp = rest.getForEntity(url, byte[].class);
            
            long duration = System.currentTimeMillis() - start;
            logger.debug("GPX downloaded in {} ms ({} bytes)", duration, gpxResp.getBody() != null ? gpxResp.getBody().length : 0);
            
            if (gpxResp.getStatusCode().value() == 401 || gpxResp.getStatusCode().value() == 403) {
                logger.warn("Token invalid for activity {}", activityId);
                return ResponseEntity.status(401)
                        .body(Map.of("error", "invalid or expired session token", "retryable", true));
            }
            
            if (!gpxResp.getStatusCode().is2xxSuccessful() || gpxResp.getBody() == null) {
                logger.error("Failed to download GPX: status {}", gpxResp.getStatusCode());
                return ResponseEntity.status(502).body(Map.of("error", "Failed to download GPX"));
            }
            gpxBytes = gpxResp.getBody();
            logger.info("GPX bytes received: {} bytes", gpxBytes.length);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                logger.warn("Token error for activity {}", activityId);
                return ResponseEntity.status(401)
                        .body(Map.of("error", "invalid or expired session token", "retryable", true));
            }
            logger.error("HTTP error downloading GPX: {}", e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", e.getResponseBodyAsString()));
        } catch (Exception e) {
            logger.error("Error downloading GPX", e);
            return ResponseEntity.status(502).body(Map.of("error", "Garmin service unavailable: " + e.getMessage()));
        }

        // 2. Parse GPX
        GpxUploadController.GpxData gpxData;
        try {
            logger.debug("Parsing GPX data");
            long start = System.currentTimeMillis();
            gpxData = GpxUploadController.parseGpxBytes(gpxBytes);
            long duration = System.currentTimeMillis() - start;
            logger.debug("GPX parsed in {} ms: {} points", duration, gpxData.getPoints().size());
        } catch (Exception e) {
            logger.error("GPX parse error", e);
            return ResponseEntity.badRequest().body(Map.of("error", "GPX parse error: " + e.getMessage()));
        }

        // 3. Persist track and sections
        try {
            logger.debug("Saving track to database");
            long start = System.currentTimeMillis();
            
            User user = userService.getUserByUsername(principal.getName());
            GPXTrack track = new GPXTrack();
            track.setName(name != null && !name.isBlank() ? name : gpxData.getName());
            track.setType(GPXTrackType.valueOf(type.toUpperCase()));
            track.setStatus(GPXTrackStatus.PUBLISHED);
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
            logger.info("Track saved with ID: {}, GPX size: {} bytes", saved.getId(), saved.getGpxFile() != null ? saved.getGpxFile().length : 0);
            
            // Save sections if provided
            List<UploadSectionRequest> sections = body.containsKey("sections")
                    ? mapper.convertValue(body.get("sections"), new TypeReference<List<UploadSectionRequest>>() {})
                    : List.of();
            
            int sectionCount = 0;
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
                sectionCount++;
            }
            
            long duration = System.currentTimeMillis() - start;
            long totalDuration = System.currentTimeMillis() - totalStart;
            logger.info("Activity {} imported successfully in {} ms (total: {} ms). Saved 1 main track + {} sections",
                activityId, duration, totalDuration, sectionCount);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(new TrackResponse(saved));
        } catch (Exception e) {
            logger.error("Error saving track for activity " + activityId, e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to save track: " + e.getMessage()));
        }
    }
}


