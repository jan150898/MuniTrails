package com.example.trails.dto;

import com.example.trails.model.GPXTrack;
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
    private Instant createdAt;
    private Instant updatedAt;

    public TrackResponse() {}

    public TrackResponse(GPXTrack track) {
        this.id = track.getId();
        this.name = track.getName();
        this.type = track.getType().name();
        this.status = track.getStatus().name();
        // Tests may provide a GPXTrack without visibility set.
        // Avoid NPE and let JSON omit/return null instead.
        this.visibility = track.getVisibility() != null ? track.getVisibility().name() : null;

        this.startLat = track.getStartLat();
        this.startLon = track.getStartLon();
        this.distanceMeters = track.getDistanceMeters();
        this.elevationGainMeters = track.getElevationGainMeters();
        this.elevationLossMeters = track.getElevationLossMeters();
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

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
