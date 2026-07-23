package com.example.trails.dto;

import com.example.trails.model.GPXTrack;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;
import java.util.Base64;

/**
 * Detailed tour response with all attributes for editing.
 * GPX data is transmitted as base64-encoded string in JSON.
 */
public class TourDetailResponse {
    private UUID id;
    private String name;
    private String type;
    private String description;
    private double distanceMeters;
    private double elevationGainMeters;
    private double elevationLossMeters;
    private int overallRating;
    private int exposition;
    private int uphillRating;
    private boolean rideAgain;
    private byte[] gpxData;
    private Instant createdAt;
    private Instant updatedAt;

    public TourDetailResponse() {}

    public TourDetailResponse(GPXTrack track) {
        this.id = track.getId();
        this.name = track.getName();
        this.type = track.getType() != null ? track.getType().toString() : "";
        this.distanceMeters = track.getDistanceMeters();
        this.elevationGainMeters = track.getElevationGainMeters();
        this.elevationLossMeters = track.getElevationLossMeters();
        this.overallRating = track.getOverallRating();
        this.exposition = track.getExposition();
        this.uphillRating = track.getUphillRating();
        this.rideAgain = track.isRideAgain();
        this.gpxData = track.getGpxFile();
        this.createdAt = track.getCreatedAt();
        this.updatedAt = track.getUpdatedAt();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

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

    // GPX Data: serialize/deserialize as base64 for JSON
    @JsonProperty("gpxData")
    public String getGpxDataBase64() {
        return gpxData != null ? Base64.getEncoder().encodeToString(gpxData) : null;
    }

    @JsonProperty("gpxData")
    public void setGpxDataBase64(String base64) {
        this.gpxData = base64 != null ? Base64.getDecoder().decode(base64) : null;
    }

    // Direct access for internal use
    public byte[] getGpxDataRaw() { return gpxData; }
    public void setGpxDataRaw(byte[] gpxData) { this.gpxData = gpxData; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
