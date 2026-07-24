package com.example.trails.dto;

import com.example.trails.model.GPXTrack;
import com.example.trails.model.GPXTrackStatus;
import com.example.trails.model.GPXTrackType;
import com.example.trails.model.Visibility;
import java.time.Instant;
import java.util.UUID;

public class TrackResponse {
    private UUID id;
    private String name;
    private String type;
    private String status;
    private String visibility;
    private double startLat;
    private double startLon;
    private double distanceMeters;
    private double elevationGainMeters;
    private double elevationLossMeters;
    private int overallRating;
    private int exposition;
    private int uphillRating;
    private boolean rideAgain;
    private Instant createdAt;
    private Instant updatedAt;

    public TrackResponse() {}

    /** Lightweight list projection that intentionally excludes the GPX binary data. */
    public TrackResponse(UUID id, String name, GPXTrackType type, GPXTrackStatus status,
                         Visibility visibility,
                         double startLat, double startLon, double distanceMeters, double elevationGainMeters,
                         double elevationLossMeters, int overallRating, int exposition, int uphillRating,
                         boolean rideAgain, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.type = type == null ? null : type.name();
        this.status = status == null ? null : status.name();
        this.visibility = visibility == null ? null : visibility.name();
        this.startLat = startLat;
        this.startLon = startLon;
        this.distanceMeters = distanceMeters;
        this.elevationGainMeters = elevationGainMeters;
        this.elevationLossMeters = elevationLossMeters;
        this.overallRating = overallRating;
        this.exposition = exposition;
        this.uphillRating = uphillRating;
        this.rideAgain = rideAgain;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public TrackResponse(GPXTrack track) {
        this.id = track.getId();
        this.name = track.getName();
        this.type = track.getType() != null ? track.getType().name() : null;
        this.status = track.getStatus() != null ? track.getStatus().name() : null;
        this.visibility = track.getVisibility() != null ? track.getVisibility().name() : null;
        this.startLat = track.getStartLat();
        this.startLon = track.getStartLon();
        this.distanceMeters = track.getDistanceMeters();
        this.elevationGainMeters = track.getElevationGainMeters();
        this.elevationLossMeters = track.getElevationLossMeters();
        this.overallRating = track.getOverallRating();
        this.exposition = track.getExposition();
        this.uphillRating = track.getUphillRating();
        this.rideAgain = track.isRideAgain();
        this.createdAt = track.getCreatedAt();
        this.updatedAt = track.getUpdatedAt();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public double getStartLat() { return startLat; }
    public void setStartLat(double startLat) { this.startLat = startLat; }

    public double getStartLon() { return startLon; }
    public void setStartLon(double startLon) { this.startLon = startLon; }

    public double getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(double distanceMeters) { this.distanceMeters = distanceMeters; }

    public double getElevationGainMeters() { return elevationGainMeters; }
    public void setElevationGainMeters(double elevationGainMeters) { this.elevationGainMeters = elevationGainMeters; }

    public double getElevationLossMeters() { return elevationLossMeters; }
    public void setElevationLossMeters(double elevationLossMeters) { this.elevationLossMeters = elevationLossMeters; }

    public int getOverallRating() { return overallRating; }
    public void setOverallRating(int overallRating) { this.overallRating = overallRating; }

    public int getExposition() { return exposition; }
    public void setExposition(int exposition) { this.exposition = exposition; }

    public int getUphillRating() { return uphillRating; }
    public void setUphillRating(int uphillRating) { this.uphillRating = uphillRating; }

    public boolean isRideAgain() { return rideAgain; }
    public void setRideAgain(boolean rideAgain) { this.rideAgain = rideAgain; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
