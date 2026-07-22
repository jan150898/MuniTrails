package com.example.trails.web;

import com.example.trails.dto.UploadSectionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SectionDetector
 */
@DisplayName("Section Detection Tests")
class SectionDetectorTest {

    @Test
    @DisplayName("Detect uphill section")
    void testDetectUphillSection() {
        List<GpxUploadController.TrackPoint> points = new ArrayList<>();
        points.add(new GpxUploadController.TrackPoint(47.0, 11.0, 1000));
        points.add(new GpxUploadController.TrackPoint(47.001, 11.001, 1010));
        points.add(new GpxUploadController.TrackPoint(47.002, 11.002, 1020));
        points.add(new GpxUploadController.TrackPoint(47.003, 11.003, 1030));
        points.add(new GpxUploadController.TrackPoint(47.004, 11.004, 1040));
        points.add(new GpxUploadController.TrackPoint(47.005, 11.005, 1050));

        List<UploadSectionRequest> sections = SectionDetector.detectSections(points);

        assertFalse(sections.isEmpty(), "Should detect uphill section");
        assertEquals("UPHILL", sections.get(0).getType(), "Should be uphill");
    }

    @Test
    @DisplayName("Detect downhill section")
    void testDetectDownhillSection() {
        List<GpxUploadController.TrackPoint> points = new ArrayList<>();
        points.add(new GpxUploadController.TrackPoint(47.0, 11.0, 1050));
        points.add(new GpxUploadController.TrackPoint(47.001, 11.001, 1040));
        points.add(new GpxUploadController.TrackPoint(47.002, 11.002, 1030));
        points.add(new GpxUploadController.TrackPoint(47.003, 11.003, 1020));
        points.add(new GpxUploadController.TrackPoint(47.004, 11.004, 1010));
        points.add(new GpxUploadController.TrackPoint(47.005, 11.005, 1000));

        List<UploadSectionRequest> sections = SectionDetector.detectSections(points);

        assertFalse(sections.isEmpty(), "Should detect downhill section");
        assertEquals("DOWNHILL", sections.get(0).getType(), "Should be downhill");
    }

    @Test
    @DisplayName("Detect multiple sections")
    void testDetectMultipleSections() {
        List<GpxUploadController.TrackPoint> points = new ArrayList<>();
        // Uphill 1000 -> 1050
        points.add(new GpxUploadController.TrackPoint(47.0, 11.0, 1000));
        points.add(new GpxUploadController.TrackPoint(47.001, 11.001, 1010));
        points.add(new GpxUploadController.TrackPoint(47.002, 11.002, 1020));
        points.add(new GpxUploadController.TrackPoint(47.003, 11.003, 1030));
        points.add(new GpxUploadController.TrackPoint(47.004, 11.004, 1040));
        points.add(new GpxUploadController.TrackPoint(47.005, 11.005, 1050));
        // Downhill 1050 -> 1000
        points.add(new GpxUploadController.TrackPoint(47.006, 11.006, 1040));
        points.add(new GpxUploadController.TrackPoint(47.007, 11.007, 1030));
        points.add(new GpxUploadController.TrackPoint(47.008, 11.008, 1020));
        points.add(new GpxUploadController.TrackPoint(47.009, 11.009, 1010));
        points.add(new GpxUploadController.TrackPoint(47.01, 11.01, 1000));

        List<UploadSectionRequest> sections = SectionDetector.detectSections(points);

        assertTrue(sections.size() >= 2, "Should detect at least 2 sections");
        assertEquals("UPHILL", sections.get(0).getType(), "First section should be uphill");
        assertEquals("DOWNHILL", sections.get(1).getType(), "Second section should be downhill");
    }

    @Test
    @DisplayName("Ignore flat sections (no elevation change)")
    void testIgnoreFlatSection() {
        List<GpxUploadController.TrackPoint> points = new ArrayList<>();
        points.add(new GpxUploadController.TrackPoint(47.0, 11.0, 1000));
        points.add(new GpxUploadController.TrackPoint(47.001, 11.001, 1000));
        points.add(new GpxUploadController.TrackPoint(47.002, 11.002, 1000));
        points.add(new GpxUploadController.TrackPoint(47.003, 11.003, 1000));

        List<UploadSectionRequest> sections = SectionDetector.detectSections(points);

        assertTrue(sections.isEmpty(), "Should not detect sections in flat terrain");
    }

    @Test
    @DisplayName("Handle minimal points")
    void testMinimalPoints() {
        List<GpxUploadController.TrackPoint> points = new ArrayList<>();
        points.add(new GpxUploadController.TrackPoint(47.0, 11.0, 1000));

        List<UploadSectionRequest> sections = SectionDetector.detectSections(points);

        assertTrue(sections.isEmpty(), "Should handle single point gracefully");
    }

    @Test
    @DisplayName("Handle empty list")
    void testEmptyList() {
        List<GpxUploadController.TrackPoint> points = new ArrayList<>();

        List<UploadSectionRequest> sections = SectionDetector.detectSections(points);

        assertTrue(sections.isEmpty(), "Should handle empty list gracefully");
    }

    @Test
    @DisplayName("Handle null list")
    void testNullList() {
        List<UploadSectionRequest> sections = SectionDetector.detectSections(null);

        assertTrue(sections.isEmpty(), "Should handle null list gracefully");
    }

    @Test
    @DisplayName("Extend section through brief neutral segment to peak")
    void testExtendThroughNeutralToPeak() {
        List<GpxUploadController.TrackPoint> points = new ArrayList<>();
        // Uphill, then brief neutral, then small bump to peak
        points.add(new GpxUploadController.TrackPoint(47.0, 11.0, 1000));
        points.add(new GpxUploadController.TrackPoint(47.0005, 11.0005, 1015));
        points.add(new GpxUploadController.TrackPoint(47.001, 11.001, 1030));
        points.add(new GpxUploadController.TrackPoint(47.0015, 11.0015, 1050)); // Peak
        points.add(new GpxUploadController.TrackPoint(47.002, 11.002, 1049)); // Neutral/slight dip
        points.add(new GpxUploadController.TrackPoint(47.0025, 11.0025, 1048)); // Neutral continues
        // Downhill starts
        points.add(new GpxUploadController.TrackPoint(47.003, 11.003, 1030));
        points.add(new GpxUploadController.TrackPoint(47.0035, 11.0035, 1010));
        points.add(new GpxUploadController.TrackPoint(47.004, 11.004, 990));

        List<UploadSectionRequest> sections = SectionDetector.detectSections(points);

        assertTrue(sections.size() >= 1, "Should detect sections");
        // Uphill should extend through neutral to include the peak
        UploadSectionRequest uphillSection = sections.stream()
            .filter(s -> "UPHILL".equals(s.getType()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(uphillSection, "Should detect uphill section");
        // The uphill section should extend to or past the peak
        assertTrue(uphillSection.getEndIndex() >= 3, "Uphill should extend to include peak");
    }

    @Test
    @DisplayName("Merge consecutive same-direction sections")
    void testMergeConsecutiveSections() {
        List<GpxUploadController.TrackPoint> points = new ArrayList<>();
        // Two uphills with small flat between (should merge into one)
        points.add(new GpxUploadController.TrackPoint(47.0, 11.0, 1000));
        points.add(new GpxUploadController.TrackPoint(47.001, 11.001, 1020));
        points.add(new GpxUploadController.TrackPoint(47.002, 11.002, 1030));
        points.add(new GpxUploadController.TrackPoint(47.003, 11.003, 1030)); // Neutral
        points.add(new GpxUploadController.TrackPoint(47.004, 11.004, 1040));
        points.add(new GpxUploadController.TrackPoint(47.005, 11.005, 1050));

        List<UploadSectionRequest> sections = SectionDetector.detectSections(points);

        // Should have 1 uphill section (not split into two)
        long uphillCount = sections.stream().filter(s -> "UPHILL".equals(s.getType())).count();
        assertTrue(uphillCount <= 1, "Consecutive uphills should be merged into one section");
    }

    @Test
    @DisplayName("Preserve section names")
    void testSectionNaming() {
        List<GpxUploadController.TrackPoint> points = new ArrayList<>();
        points.add(new GpxUploadController.TrackPoint(47.0, 11.0, 1000));
        points.add(new GpxUploadController.TrackPoint(47.001, 11.001, 1015));
        points.add(new GpxUploadController.TrackPoint(47.002, 11.002, 1030));
        points.add(new GpxUploadController.TrackPoint(47.003, 11.003, 1045));
        points.add(new GpxUploadController.TrackPoint(47.004, 11.004, 1060));

        List<UploadSectionRequest> sections = SectionDetector.detectSections(points);

        assertFalse(sections.isEmpty(), "Should detect section");
        assertNotNull(sections.get(0).getName(), "Section should have name");
        assertTrue(sections.get(0).getName().contains("Uphill") || 
                   sections.get(0).getName().contains("Downhill"), 
                   "Section name should indicate direction");
    }
}
