package com.example.trails.dto;

import com.example.trails.model.Visibility;
import java.util.List;

/**
 * Request to update an existing tour with new attributes and sections.
 */
public class UpdateTourRequest {
    private String name;
    private String type;
    private String description;
    private Visibility visibility;
    private int overallRating;
    private int exposition;
    private int uphillRating;
    private boolean rideAgain;
    private List<UploadSectionRequest> sections;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Visibility getVisibility() { return visibility; }
    public void setVisibility(Visibility visibility) { this.visibility = visibility; }

    public int getOverallRating() { return overallRating; }
    public void setOverallRating(int overallRating) { this.overallRating = overallRating; }

    public int getExposition() { return exposition; }
    public void setExposition(int exposition) { this.exposition = exposition; }

    public int getUphillRating() { return uphillRating; }
    public void setUphillRating(int uphillRating) { this.uphillRating = uphillRating; }

    public boolean isRideAgain() { return rideAgain; }
    public void setRideAgain(boolean rideAgain) { this.rideAgain = rideAgain; }

    public List<UploadSectionRequest> getSections() { return sections; }
    public void setSections(List<UploadSectionRequest> sections) { this.sections = sections; }
}
