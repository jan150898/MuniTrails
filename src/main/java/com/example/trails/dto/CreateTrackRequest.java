package com.example.trails.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateTrackRequest {
    @NotBlank(message = "Track name is required")
    private String name;

    @NotBlank(message = "Track type is required")
    private String type;

    private double startLat;
    private double startLon;
    private double distanceMeters;
    private double elevationGainMeters;
    private double elevationLossMeters;

    public CreateTrackRequest() {}

    public CreateTrackRequest(String name, String type, double startLat, double startLon,
                             double distanceMeters, double elevationGainMeters, double elevationLossMeters) {
        this.name = name;
        this.type = type;
        this.startLat = startLat;
        this.startLon = startLon;
        this.distanceMeters = distanceMeters;
        this.elevationGainMeters = elevationGainMeters;
        this.elevationLossMeters = elevationLossMeters;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

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
}
