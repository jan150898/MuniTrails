package com.example.trails.dto;

/** A user-reviewed uphill or downhill extracted from an uploaded GPX track. */
public class UploadSectionRequest {
    private int startIndex;
    private int endIndex;
    private String name;
    private String type;
    private boolean included = true;
    private int overallRating;
    private int exposition = 5;
    private int uphillRating = 5;
    private boolean rideAgain;

    public int getStartIndex() { return startIndex; }
    public void setStartIndex(int startIndex) { this.startIndex = startIndex; }
    public int getEndIndex() { return endIndex; }
    public void setEndIndex(int endIndex) { this.endIndex = endIndex; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public boolean isIncluded() { return included; }
    public void setIncluded(boolean included) { this.included = included; }
    public int getOverallRating() { return overallRating; }
    public void setOverallRating(int overallRating) { this.overallRating = overallRating; }
    public int getExposition() { return exposition; }
    public void setExposition(int exposition) { this.exposition = exposition; }
    public int getUphillRating() { return uphillRating; }
    public void setUphillRating(int uphillRating) { this.uphillRating = uphillRating; }
    public boolean isRideAgain() { return rideAgain; }
    public void setRideAgain(boolean rideAgain) { this.rideAgain = rideAgain; }
}
