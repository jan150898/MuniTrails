package com.example.trails.web;

import com.example.trails.dto.UploadSectionRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Section detection helper - detects uphill/downhill sections from GPX data.
 * Improved deduplication: merges overlapping sections across TOUR, UPHILL, DOWNHILL types.
 */
public class SectionDetector {

    private static final double OVERLAP_THRESHOLD = 0.8; // 80% overlap = same section
    private static final double TOUR_OVERLAP_THRESHOLD = 0.5; // 50% for TOUR vs sections
    private static final int MIN_ELEVATION_GAIN = 50;    // meters
    private static final int MIN_DISTANCE = 150;         // meters
    private static final double ELEVATION_THRESHOLD_BASE = 0.3;
    private static final double ELEVATION_THRESHOLD_FACTOR = 1.0;

    /**
     * Finds sustained elevation changes from GPX track points.
     * Uphill sections extend through brief flat/neutral segments to the peak.
     * Downhill sections extend through brief flat segments to the lowest point.
     * Improved: merges overlapping and duplicate sections across all types (TOUR, UPHILL, DOWNHILL).
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
            double threshold = ELEVATION_THRESHOLD_BASE + (distMeters / 1000.0) * ELEVATION_THRESHOLD_FACTOR;

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

        // Close active section at end of tour
        if (currentDirection != 0) {
            int endIndex = points.size() - 1;
            if (neutralStartIndex != -1) {
                endIndex = findExtreme(points, neutralStartIndex, points.size() - 1, currentDirection);
            }
            addSectionIfMeaningful(sections, points, currentStartIndex, endIndex, currentDirection);
        }

        // Merge overlapping and duplicate sections
        return deduplicateSections(sections, points);
    }

    /**
     * Merge sections that are duplicates or overlapping.
     * Checks overlap across TOUR, UPHILL, and DOWNHILL types:
     * - UPHILL vs UPHILL: 80% overlap = same section
     * - DOWNHILL vs DOWNHILL: 80% overlap = same section
     * - UPHILL vs TOUR: 50% overlap = UPHILL is part of TOUR
     * - DOWNHILL vs TOUR: 50% overlap = DOWNHILL is part of TOUR
     */
    private static List<UploadSectionRequest> deduplicateSections(List<UploadSectionRequest> sections,
                                                                   List<GpxUploadController.TrackPoint> points) {
        List<UploadSectionRequest> merged = new ArrayList<>();
        
        for (UploadSectionRequest newSection : sections) {
            boolean isDuplicate = false;
            
            // Check against all existing merged sections
            for (int i = 0; i < merged.size(); i++) {
                UploadSectionRequest existing = merged.get(i);
                
                double overlapRatio = calculateOverlapRatio(existing, newSection);
                double threshold = getOverlapThreshold(existing.getType(), newSection.getType());
                
                if (overlapRatio >= threshold) {
                    // Determine merge strategy based on types
                    if (existing.getType().equals(newSection.getType())) {
                        // Same type: keep the longer one
                        if (newSection.getEndIndex() - newSection.getStartIndex() >
                            existing.getEndIndex() - existing.getStartIndex()) {
                            merged.set(i, newSection);
                        }
                    } else if (isTourContainingSection(existing, newSection)) {
                        // TOUR contains section: keep TOUR
                        // (TOUR should remain unchanged)
                    } else if (isTourContainingSection(newSection, existing)) {
                        // Section is part of TOUR: keep TOUR
                        existing.setStartIndex(Math.min(existing.getStartIndex(), newSection.getStartIndex()));
                        existing.setEndIndex(Math.max(existing.getEndIndex(), newSection.getEndIndex()));
                    } else if ("TOUR".equals(existing.getType()) || "TOUR".equals(newSection.getType())) {
                        // One is TOUR, merge by expanding boundaries
                        int start = Math.min(existing.getStartIndex(), newSection.getStartIndex());
                        int end = Math.max(existing.getEndIndex(), newSection.getEndIndex());
                        existing.setStartIndex(start);
                        existing.setEndIndex(end);
                    } else {
                        // Both UPHILL or both DOWNHILL: keep longer
                        if (newSection.getEndIndex() - newSection.getStartIndex() >
                            existing.getEndIndex() - existing.getStartIndex()) {
                            merged.set(i, newSection);
                        }
                    }
                    isDuplicate = true;
                    break;
                }
            }
            
            if (!isDuplicate) {
                merged.add(newSection);
            }
        }
        
        return mergeConsecutiveSameType(merged);
    }

    /**
     * Determine overlap threshold based on types being compared.
     * - Same type (UPHILL-UPHILL, etc.): 80% threshold
     * - TOUR involved: 50% threshold (sections can be part of TOUR)
     */
    private static double getOverlapThreshold(String type1, String type2) {
        if (type1.equals(type2)) {
            return OVERLAP_THRESHOLD;  // 80%
        }
        if ("TOUR".equals(type1) || "TOUR".equals(type2)) {
            return TOUR_OVERLAP_THRESHOLD;  // 50%
        }
        return 1.0;  // No merge for unrelated types
    }

    /**
     * Check if one section type can contain another.
     */
    private static boolean isTourContainingSection(UploadSectionRequest container, UploadSectionRequest section) {
        if (!"TOUR".equals(container.getType())) {
            return false;
        }
        if ("TOUR".equals(section.getType())) {
            return false;
        }
        
        // TOUR contains section if section is fully within TOUR bounds
        return section.getStartIndex() >= container.getStartIndex() &&
               section.getEndIndex() <= container.getEndIndex();
    }

    /**
     * Calculate overlap ratio between two sections.
     * Returns 0-1 where 1 means identical, 0 means no overlap.
     */
    private static double calculateOverlapRatio(UploadSectionRequest section1, UploadSectionRequest section2) {
        int start1 = section1.getStartIndex();
        int end1 = section1.getEndIndex();
        int start2 = section2.getStartIndex();
        int end2 = section2.getEndIndex();
        
        // Calculate overlap
        int overlapStart = Math.max(start1, start2);
        int overlapEnd = Math.min(end1, end2);
        
        if (overlapEnd < overlapStart) {
            return 0; // No overlap
        }
        
        int overlapLength = overlapEnd - overlapStart;
        int len1 = end1 - start1;
        int len2 = end2 - start2;
        int maxLen = Math.max(len1, len2);
        
        return (double) overlapLength / maxLen;
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
        if (Math.abs(elevChange) < MIN_ELEVATION_GAIN || distance < MIN_DISTANCE) return;

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
