package com.example.trails.web;

import com.example.trails.dto.UploadSectionRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Section detection helper - detects uphill/downhill sections from GPX data.
 * Fixed to extend uphill sections through brief neutral segments to reach the peak.
 */
public class SectionDetector {

    /**
     * Finds sustained elevation changes from GPX track points.
     * Uphill sections extend through brief flat/neutral segments to the peak.
     * Downhill sections extend through brief flat segments to the lowest point.
     */
    static List<UploadSectionRequest> detectSections(List<GpxUploadController.TrackPoint> points) {
        List<UploadSectionRequest> sections = new ArrayList<>();
        if (points == null || points.size() < 2) return sections;

        int currentDirection = 0;  // 1=uphill, -1=downhill, 0=neutral
        int currentStartIndex = -1;
        int neutralStartIndex = -1;

        for (int i = 1; i < points.size(); i++) {
            GpxUploadController.TrackPoint prev = points.get(i - 1);
            GpxUploadController.TrackPoint curr = points.get(i);
            double elevChange = curr.getElevation() - prev.getElevation();

            // Distance-dependent threshold
            double distMeters = haversineDistance(prev.getLat(), prev.getLon(), 
                                                   curr.getLat(), curr.getLon()) * 1000.0;
            double threshold = 0.3 + (distMeters / 1000.0) * 1.0;

            int nextDirection = elevChange > threshold ? 1 : elevChange < -threshold ? -1 : 0;

            // Handle neutral segment
            if (nextDirection == 0) {
                if (currentDirection != 0 && neutralStartIndex == -1) {
                    neutralStartIndex = i - 1;  // Mark start of neutral zone
                }
                continue;
            }

            // Non-neutral direction
            if (currentDirection == 0) {
                // Starting fresh
                currentDirection = nextDirection;
                currentStartIndex = i - 1;
                neutralStartIndex = -1;
            } else if (currentDirection == nextDirection) {
                // Continuing same direction - absorb any neutral segment
                neutralStartIndex = -1;
            } else {
                // Direction change - close previous section at peak/low
                int endIndex = neutralStartIndex != -1 
                    ? findExtreme(points, neutralStartIndex, i - 1, currentDirection)
                    : i - 1;
                addSectionIfMeaningful(sections, points, currentStartIndex, endIndex, currentDirection);
                
                // Start new section
                currentDirection = nextDirection;
                currentStartIndex = i - 1;
                neutralStartIndex = -1;
            }
        }

        
        if (currentDirection != 0) {
            int endIndex = points.size() - 1;
            if (neutralStartIndex != -1) {
                endIndex = findExtreme(points, neutralStartIndex, points.size() - 1, currentDirection);
            }
            addSectionIfMeaningful(sections, points, currentStartIndex, endIndex, currentDirection);
        }

        return mergeConsecutiveSameType(sections);
    }

    /**
     * Find the peak (uphill) or lowest point (downhill) within a range.
     */
    private static int findExtreme(List<GpxUploadController.TrackPoint> points, int start, int end, int direction) {
        if (start >= end) return start;
        int extremeIdx = start;
        double extremeElev = points.get(start).getElevation();
        
        for (int i = start + 1; i <= end; i++) {
            double elev = points.get(i).getElevation();
            if (direction > 0 && elev > extremeElev) {
                extremeElev = elev;
                extremeIdx = i;
            } else if (direction < 0 && elev < extremeElev) {
                extremeElev = elev;
                extremeIdx = i;
            }
        }
        return extremeIdx;
    }

    /**
     * Merge consecutive sections with the same type.
     */
    private static List<UploadSectionRequest> mergeConsecutiveSameType(List<UploadSectionRequest> sections) {
        List<UploadSectionRequest> merged = new ArrayList<>();
        for (UploadSectionRequest section : sections) {
            if (!merged.isEmpty()) {
                UploadSectionRequest last = merged.get(merged.size() - 1);
                if (last.getType() != null && last.getType().equalsIgnoreCase(section.getType())) {
                    last.setEndIndex(Math.max(last.getEndIndex(), section.getEndIndex()));
                    continue;
                }
            }
            merged.add(section);
        }
        return merged;
    }

    /**
     * Add section only if it meets minimum elevation/distance criteria.
     */
    private static void addSectionIfMeaningful(List<UploadSectionRequest> sections,
            List<GpxUploadController.TrackPoint> points, int start, int end, int direction) {
        if (end <= start) return;

        double elevChange = points.get(end).getElevation() - points.get(start).getElevation();
        double distance = 0;
        for (int i = start + 1; i <= end; i++) {
            GpxUploadController.TrackPoint p = points.get(i - 1);
            GpxUploadController.TrackPoint c = points.get(i);
            distance += haversineDistance(p.getLat(), p.getLon(), c.getLat(), c.getLon()) * 1000;
        }

        // Thresholds: 50m elevation gain/loss, 150m distance
        if (Math.abs(elevChange) < 50 || distance < 150) return;

        UploadSectionRequest section = new UploadSectionRequest();
        section.setStartIndex(start);
        section.setEndIndex(end);
        section.setType(direction > 0 ? "UPHILL" : "DOWNHILL");
        section.setName((direction > 0 ? "Uphill" : "Downhill") + " " + (sections.size() + 1));
        sections.add(section);
    }

    private static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
