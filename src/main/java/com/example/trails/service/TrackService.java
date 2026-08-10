package com.example.trails.service;

import com.example.trails.model.GPXTrack;
import com.example.trails.model.GPXTrackStatus;
import com.example.trails.model.GPXTrackType;
import com.example.trails.model.User;
import com.example.trails.model.Visibility;
import com.example.trails.dto.TrackResponse;
import com.example.trails.dto.UpdateTourRequest;
import com.example.trails.repo.GPXTrackRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TrackService {

    private final GPXTrackRepository gpxTrackRepository;

    public TrackService(GPXTrackRepository gpxTrackRepository) {
        this.gpxTrackRepository = gpxTrackRepository;
    }

    public List<GPXTrack> findAll() {
        return gpxTrackRepository.findAll();
    }

    public List<TrackResponse> findAllSummaries() {
        return gpxTrackRepository.findAllSummaries();
    }

    public List<TrackResponse> findVisibleSummaries(User user) {
        return gpxTrackRepository.findAll().stream()
                .filter(track -> canRead(track, user))
                .map(TrackResponse::new)
                .collect(Collectors.toList());
    }

    public List<TrackResponse> findSummariesByCreatorId(UUID userId) {
        return gpxTrackRepository.findSummariesByCreatorId(userId);
    }

    public Optional<TrackResponse> findSummaryById(UUID trackId) {
        return gpxTrackRepository.findSummaryById(trackId);
    }

    public Page<GPXTrack> findAllPaginated(User user, Pageable pageable) {
        List<GPXTrack> visible = gpxTrackRepository.findAll().stream()
                .filter(track -> canRead(track, user))
                .collect(Collectors.toList());
        int start = Math.min((int) pageable.getOffset(), visible.size());
        int end = Math.min(start + pageable.getPageSize(), visible.size());
        return new PageImpl<>(visible.subList(start, end), pageable, visible.size());
    }

    public boolean canRead(GPXTrack track, User user) {
        return track.getVisibility() == Visibility.PUBLIC || isOwnerOrAdmin(track, user);
    }

    public boolean isOwnerOrAdmin(GPXTrack track, User user) {
        return user != null && ("ROLE_ADMIN".equals(user.getRole()) || "ADMIN".equals(user.getRole())
                || track.getCreatedBy().getId().equals(user.getId()));
    }

    public GPXTrack requireReadable(UUID id, User user) {
        GPXTrack track = findById(id);
        if (!canRead(track, user)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to access this track");
        return track;
    }

    public GPXTrack findById(UUID id) {
        return gpxTrackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Track not found: " + id));
    }

    public List<GPXTrack> filterTracks(
            Double minDistance,
            Double maxDistance,
            String trackType,
            Double minElevationGain,
            Double maxElevationGain,
            Double minHighestPoint,
            Double maxHighestPoint,
            Integer minRating,
            Integer maxRating,
            Integer minExposition,
            Integer maxExposition,
            Boolean rideAgain, User user) {
        
        List<GPXTrack> tracks = gpxTrackRepository.findAll();
        
        return tracks.stream()
                .filter(t -> canRead(t, user))
                .filter(t -> minDistance == null || t.getDistanceMeters() >= minDistance)
                .filter(t -> maxDistance == null || t.getDistanceMeters() <= maxDistance)
                .filter(t -> trackType == null || t.getType().name().equalsIgnoreCase(trackType))
                .filter(t -> minElevationGain == null || t.getElevationGainMeters() >= minElevationGain)
                .filter(t -> maxElevationGain == null || t.getElevationGainMeters() <= maxElevationGain)
                .filter(t -> minHighestPoint == null || t.getHighestPointAltitudeMeters() >= minHighestPoint)
                .filter(t -> maxHighestPoint == null || t.getHighestPointAltitudeMeters() <= maxHighestPoint)
                .filter(t -> minRating == null || t.getOverallRating() >= minRating)
                .filter(t -> maxRating == null || t.getOverallRating() <= maxRating)
                .filter(t -> minExposition == null || t.getExposition() >= minExposition)
                .filter(t -> maxExposition == null || t.getExposition() <= maxExposition)
                .filter(t -> rideAgain == null || t.isRideAgain() == rideAgain)
                .collect(Collectors.toList());
    }

    public GPXTrack createTrack(String name, GPXTrackType type, double startLat, double startLon,
                                double distanceMeters, double elevGainMeters, double elevLossMeters,
                                User createdBy) {
        GPXTrack track = new GPXTrack();
        track.setName(name);
        track.setType(type);
        track.setStatus(GPXTrackStatus.DRAFT);
        track.setCreatedBy(createdBy);
        track.setLastEditedBy(createdBy);
        track.setStartLat(startLat);
        track.setStartLon(startLon);
        track.setDistanceMeters(distanceMeters);
        track.setElevationGainMeters(elevGainMeters);
        track.setElevationLossMeters(elevLossMeters);
        track.setOverallRating(0);
        track.setExposition(0);
        track.setUphillRating(0);
        track.setRideAgain(false);
        return gpxTrackRepository.save(track);
    }

    public GPXTrack updateTrack(UUID id, String name, double startLat, double startLon,
                                double distanceMeters, double elevGainMeters, 
                                double elevLossMeters, User lastEditedBy) {
        GPXTrack track = findById(id);
        if (!isOwnerOrAdmin(track, lastEditedBy)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to edit this track");
        track.setName(name);
        track.setStartLat(startLat);
        track.setStartLon(startLon);
        track.setDistanceMeters(distanceMeters);
        track.setElevationGainMeters(elevGainMeters);
        track.setElevationLossMeters(elevLossMeters);
        track.setLastEditedBy(lastEditedBy);
        return gpxTrackRepository.save(track);
    }

    public void deleteTrack(UUID id, User user) {
        GPXTrack track = findById(id);
        if (!isOwnerOrAdmin(track, user)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to delete this track");
        gpxTrackRepository.delete(track);
    }

    public GPXTrack updateTourDetails(UUID trackId, UpdateTourRequest req, User user) {
        GPXTrack track = findById(trackId);
        if (!isOwnerOrAdmin(track, user)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to edit this track");
        
        if (req.getName() != null) track.setName(req.getName());
        if (req.getType() != null) track.setType(GPXTrackType.valueOf(req.getType().toUpperCase()));
        if (req.getVisibility() != null) track.setVisibility(req.getVisibility());
        track.setOverallRating(req.getOverallRating());
        track.setExposition(req.getExposition());
        track.setUphillRating(req.getUphillRating());
        track.setRideAgain(req.isRideAgain());
        track.setLastEditedBy(user);
        
        return gpxTrackRepository.save(track);
    }
}
