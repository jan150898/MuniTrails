package com.example.trails.service;

import com.example.trails.model.GPXTrack;
import com.example.trails.model.GPXTrackStatus;
import com.example.trails.model.GPXTrackType;
import com.example.trails.model.User;
import com.example.trails.dto.TrackResponse;
import com.example.trails.dto.UpdateTourRequest;
import com.example.trails.repo.GPXTrackRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
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

    public List<TrackResponse> findSummariesByCreatorId(UUID userId) {
        return gpxTrackRepository.findSummariesByCreatorId(userId);
    }

    public Optional<TrackResponse> findSummaryById(UUID trackId) {
        return gpxTrackRepository.findSummaryById(trackId);
    }

    public Page<GPXTrack> findAllPaginated(Pageable pageable) {
        return gpxTrackRepository.findAll(pageable);
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
            Boolean rideAgain) {
        
        List<GPXTrack> tracks = gpxTrackRepository.findAll();
        
        return tracks.stream()
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
        track.setName(name);
        track.setStartLat(startLat);
        track.setStartLon(startLon);
        track.setDistanceMeters(distanceMeters);
        track.setElevationGainMeters(elevGainMeters);
        track.setElevationLossMeters(elevLossMeters);
        track.setLastEditedBy(lastEditedBy);
        return gpxTrackRepository.save(track);
    }

    public void deleteTrack(UUID id) {
        gpxTrackRepository.deleteById(id);
    }

    public GPXTrack updateTourDetails(UUID trackId, UpdateTourRequest req, User user) {
        GPXTrack track = findById(trackId);
        
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
