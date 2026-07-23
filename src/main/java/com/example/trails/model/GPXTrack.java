package com.example.trails.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gpx_track", indexes = {
        @Index(name = "idx_gpx_track_name", columnList = "name")
})
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "track_kind")
public class GPXTrack {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    // The production schema uses the legacy `type` column.  Keep this explicit
    // so Hibernate does not leave it null while writing the newer track_type column.
    @Column(name = "type", nullable = false, length = 32)
    private GPXTrackType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private GPXTrackStatus status;

    

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    // Existing installations use created_by_id; use it for all new tracks.
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "last_edited_by_id")
    private User lastEditedBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    // --- Starting point (coordinates) ---
    @Column(nullable = false)
    private double startLat;

    @Column(nullable = false)
    private double startLon;

    // --- Geometry / GPX ---
    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] gpxFile; // store uploaded GPX bytes for now

    @Column(length = 64)
    private String gpxFileChecksum;

    // --- Audit / Technical data (initial subset; extend later) ---
    private double distanceMeters;
    private double elevationGainMeters;
    private double elevationLossMeters;
    private double highestPointAltitudeMeters;
    private double lowestPointAltitudeMeters;

    @Column(length = 64)
    private String boundingBox; // e.g. "minLat,minLon,maxLat,maxLon" for now

    // --- Evaluation (initial subset) ---
    private int overallRating; // 0..10
    private int exposition;     // 0..10
    private int uphillRating;  // 0..10
    private boolean rideAgain; // true/false (extend later to Maybe)

    @Column(length = 2)
    private String difficultyMin;

    @Column(length = 2)
    private String difficultyMax;

    public GPXTrack() {}


    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public GPXTrackType getType() {
        return type;
    }

    public GPXTrackStatus getStatus() {
        return status;
    }

    

    public User getCreatedBy() {
        return createdBy;
    }

    public User getLastEditedBy() {
        return lastEditedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public double getStartLat() {
        return startLat;
    }

    public double getStartLon() {
        return startLon;
    }

    public byte[] getGpxFile() {
        return gpxFile;
    }

    public String getGpxFileChecksum() {
        return gpxFileChecksum;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public double getElevationGainMeters() {
        return elevationGainMeters;
    }

    public double getElevationLossMeters() {
        return elevationLossMeters;
    }

    public double getHighestPointAltitudeMeters() {
        return highestPointAltitudeMeters;
    }

    public double getLowestPointAltitudeMeters() {
        return lowestPointAltitudeMeters;
    }

    public String getBoundingBox() {
        return boundingBox;
    }

    public int getOverallRating() {
        return overallRating;
    }

    public int getExposition() {
        return exposition;
    }

    public int getUphillRating() {
        return uphillRating;
    }

    public boolean isRideAgain() {
        return rideAgain;
    }

    public String getDifficultyMin() { return difficultyMin; }
    public String getDifficultyMax() { return difficultyMax; }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(GPXTrackType type) {
        this.type = type;
    }

    public void setStatus(GPXTrackStatus status) {
        this.status = status;
    }

    

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public void setLastEditedBy(User lastEditedBy) {
        this.lastEditedBy = lastEditedBy;
    }

    public void setStartLat(double startLat) {
        this.startLat = startLat;
    }

    public void setStartLon(double startLon) {
        this.startLon = startLon;
    }

    public void setGpxFile(byte[] gpxFile) {
        this.gpxFile = gpxFile;
    }

    public void setGpxFileChecksum(String gpxFileChecksum) {
        this.gpxFileChecksum = gpxFileChecksum;
    }

    public void setDistanceMeters(double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public void setElevationGainMeters(double elevationGainMeters) {
        this.elevationGainMeters = elevationGainMeters;
    }

    public void setElevationLossMeters(double elevationLossMeters) {
        this.elevationLossMeters = elevationLossMeters;
    }

    public void setHighestPointAltitudeMeters(double highestPointAltitudeMeters) {
        this.highestPointAltitudeMeters = highestPointAltitudeMeters;
    }

    public void setLowestPointAltitudeMeters(double lowestPointAltitudeMeters) {
        this.lowestPointAltitudeMeters = lowestPointAltitudeMeters;
    }

    public void setBoundingBox(String boundingBox) {
        this.boundingBox = boundingBox;
    }

    public void setOverallRating(int overallRating) {
        this.overallRating = overallRating;
    }

    public void setExposition(int exposition) {
        this.exposition = exposition;
    }

    public void setUphillRating(int uphillRating) {
        this.uphillRating = uphillRating;
    }

    public void setRideAgain(boolean rideAgain) {
        this.rideAgain = rideAgain;
    }

    public void setDifficultyMin(String difficultyMin) { this.difficultyMin = difficultyMin; }
    public void setDifficultyMax(String difficultyMax) { this.difficultyMax = difficultyMax; }
}


