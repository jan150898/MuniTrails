package com.example.trails.repo;

import com.example.trails.model.GPXTrack;
import com.example.trails.dto.TrackResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GPXTrackRepository extends JpaRepository<GPXTrack, UUID> {
    @Query("select new com.example.trails.dto.TrackResponse(t.id, t.name, t.type, t.status, t.visibility, t.startLat, t.startLon, t.distanceMeters, t.elevationGainMeters, t.elevationLossMeters, t.overallRating, t.exposition, t.uphillRating, t.rideAgain, t.createdAt, t.updatedAt) from GPXTrack t")
    List<TrackResponse> findAllSummaries();

    @Query("select new com.example.trails.dto.TrackResponse(t.id, t.name, t.type, t.status, t.visibility, t.startLat, t.startLon, t.distanceMeters, t.elevationGainMeters, t.elevationLossMeters, t.overallRating, t.exposition, t.uphillRating, t.rideAgain, t.createdAt, t.updatedAt) from GPXTrack t where t.createdBy.id = :userId")
    List<TrackResponse> findSummariesByCreatorId(@Param("userId") UUID userId);

    @Query("select new com.example.trails.dto.TrackResponse(t.id, t.name, t.type, t.status, t.visibility, t.startLat, t.startLon, t.distanceMeters, t.elevationGainMeters, t.elevationLossMeters, t.overallRating, t.exposition, t.uphillRating, t.rideAgain, t.createdAt, t.updatedAt) from GPXTrack t where t.id = :trackId")
    Optional<TrackResponse> findSummaryById(@Param("trackId") UUID trackId);
}

