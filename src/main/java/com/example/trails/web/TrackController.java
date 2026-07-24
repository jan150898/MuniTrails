package com.example.trails.web;

import com.example.trails.dto.CreateCommentRequest;
import com.example.trails.dto.CreateTrackRequest;
import com.example.trails.dto.CommentResponse;
import com.example.trails.dto.TrackResponse;
import com.example.trails.dto.TourDetailResponse;
import com.example.trails.dto.UpdateTourRequest;
import com.example.trails.model.Comment;
import com.example.trails.model.GPXTrack;
import com.example.trails.model.GPXTrackType;
import com.example.trails.model.User;
import com.example.trails.service.CommentService;
import com.example.trails.service.TrackService;
import com.example.trails.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class TrackController {

    private final TrackService trackService;
    private final CommentService commentService;
    private final UserService userService;

    public TrackController(TrackService trackService,
                          CommentService commentService,
                          UserService userService) {
        this.trackService = trackService;
        this.commentService = commentService;
        this.userService = userService;
    }

    private User getCurrentUser(Principal principal) {
        return userService.getUserByUsername(principal.getName());
    }

    // Track endpoints
    @GetMapping("/tracks")
    public ResponseEntity<List<TrackResponse>> listTracks() {
        return ResponseEntity.ok(trackService.findAllSummaries());
    }

    @GetMapping("/tracks/paginated")
    public ResponseEntity<TrackPageResponse> listTracksPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<GPXTrack> tracks = trackService.findAllPaginated(pageable);
        
        List<TrackResponse> responses = tracks.stream()
                .map(TrackResponse::new)
                .collect(Collectors.toList());
        
        TrackPageResponse pageResponse = new TrackPageResponse();
        pageResponse.setContent(responses);
        pageResponse.setTotalPages(tracks.getTotalPages());
        pageResponse.setTotalElements(tracks.getTotalElements());
        pageResponse.setCurrentPage(page);
        
        return ResponseEntity.ok(pageResponse);
    }

    @GetMapping("/tracks/advanced-filter")
    public ResponseEntity<List<TrackResponse>> advancedFilterTracks(
            @RequestParam(required = false) Double minDistance,
            @RequestParam(required = false) Double maxDistance,
            @RequestParam(required = false) String trackType,
            @RequestParam(required = false) Double minElevationGain,
            @RequestParam(required = false) Double maxElevationGain,
            @RequestParam(required = false) Double minHighestPoint,
            @RequestParam(required = false) Double maxHighestPoint,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating,
            @RequestParam(required = false) Integer minExposition,
            @RequestParam(required = false) Integer maxExposition,
            @RequestParam(required = false) Boolean rideAgain) {
        
        List<GPXTrack> tracks = trackService.filterTracks(
                minDistance, maxDistance, trackType,
                minElevationGain, maxElevationGain,
                minHighestPoint, maxHighestPoint,
                minRating, maxRating,
                minExposition, maxExposition,
                rideAgain
        );
        
        List<TrackResponse> responses = tracks.stream()
                .map(TrackResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/tracks/filter")
    public ResponseEntity<List<TrackResponse>> filterTracks(
            @RequestParam(required = false) Double minDistance,
            @RequestParam(required = false) Double maxDistance,
            @RequestParam(required = false) String trackType) {
        List<GPXTrack> tracks = trackService.filterTracks(minDistance, maxDistance, trackType,
                null, null, null, null, null, null, null, null, null);
        List<TrackResponse> responses = tracks.stream()
                .map(TrackResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/tracks/{trackId}")
    public ResponseEntity<TrackResponse> getTrack(@PathVariable UUID trackId) {
        GPXTrack track = trackService.findById(trackId);
        return ResponseEntity.ok(new TrackResponse(track));
    }

    @GetMapping("/tracks/{trackId}/details")
    public ResponseEntity<TourDetailResponse> getTourDetails(@PathVariable UUID trackId, Principal principal) {
        User user = getCurrentUser(principal);
        GPXTrack track = trackService.findById(trackId);
        
        if (!track.getCreatedBy().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(new TourDetailResponse(track));
    }

    @PostMapping("/tracks")
    public ResponseEntity<TrackResponse> createTrack(
            @Valid @RequestBody CreateTrackRequest req,
            Principal principal) {
        User user = getCurrentUser(principal);
        
        GPXTrack track = trackService.createTrack(
                req.getName(),
                GPXTrackType.valueOf(req.getType().toUpperCase()),
                req.getStartLat(),
                req.getStartLon(),
                req.getDistanceMeters(),
                req.getElevationGainMeters(),
                req.getElevationLossMeters(),
                user
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(new TrackResponse(track));
    }

    @PutMapping("/tracks/{trackId}")
    public ResponseEntity<TrackResponse> updateTrack(
            @PathVariable UUID trackId,
            @Valid @RequestBody CreateTrackRequest req,
            Principal principal) {
        User user = getCurrentUser(principal);
        
        GPXTrack track = trackService.updateTrack(
                trackId,
                req.getName(),
                req.getStartLat(),
                req.getStartLon(),
                req.getDistanceMeters(),
                req.getElevationGainMeters(),
                req.getElevationLossMeters(),
                user
        );
        
        return ResponseEntity.ok(new TrackResponse(track));
    }

    @PutMapping("/tracks/{trackId}/edit")
    public ResponseEntity<TourDetailResponse> editTour(
            @PathVariable UUID trackId,
            @Valid @RequestBody UpdateTourRequest req,
            Principal principal) {
        User user = getCurrentUser(principal);
        GPXTrack track = trackService.findById(trackId);
        
        if (!track.getCreatedBy().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            track = trackService.updateTourDetails(trackId, req, user);
            return ResponseEntity.ok(new TourDetailResponse(track));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/tracks/{trackId}")
    public ResponseEntity<Void> deleteTrack(@PathVariable UUID trackId) {
        trackService.deleteTrack(trackId);
        return ResponseEntity.noContent().build();
    }

    // Comment endpoints
    @GetMapping("/tracks/{trackId}/comments")
    public ResponseEntity<List<CommentResponse>> listComments(@PathVariable UUID trackId) {
        List<Comment> comments = commentService.findByTrackId(trackId);
        List<CommentResponse> responses = comments.stream()
                .map(CommentResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/tracks/{trackId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable UUID trackId,
            @Valid @RequestBody CreateCommentRequest req,
            Principal principal) {
        User user = getCurrentUser(principal);
        GPXTrack track = trackService.findById(trackId);
        
        Comment comment = commentService.createComment(track, user, req.getText());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(new CommentResponse(comment));
    }

    // Helper DTO for paginated response
    public static class TrackPageResponse {
        private List<TrackResponse> content;
        private int totalPages;
        private long totalElements;
        private int currentPage;

        public List<TrackResponse> getContent() { return content; }
        public void setContent(List<TrackResponse> content) { this.content = content; }

        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

        public long getTotalElements() { return totalElements; }
        public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

        public int getCurrentPage() { return currentPage; }
        public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
    }

    @GetMapping("/tracks/{trackId}/download-gpx")
    public ResponseEntity<byte[]> downloadGpx(@PathVariable UUID trackId) {
        GPXTrack track = trackService.findById(trackId);
        byte[] gpxData = track.getGpxFile();
        
        if (gpxData == null || gpxData.length == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        String filename = track.getName().replaceAll("[^a-zA-Z0-9._-]", "_") + ".gpx";
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .header("Content-Type", "application/gpx+xml")
                .body(gpxData);
    }}


