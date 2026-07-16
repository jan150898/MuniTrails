package com.example.trails.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GPX Parsing and Section Detection Tests")
class GpxParsingTest {

    private byte[] validGpxBytes;
    private byte[] gpxWithElevationChanges;

    @BeforeEach
    void setUp() {
        // Basic valid GPX
        validGpxBytes = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<gpx version=\"1.1\" creator=\"Test\">\n" +
                "  <metadata><name>Simple Track</name></metadata>\n" +
                "  <trk>\n" +
                "    <trkseg>\n" +
                "      <trkpt lat=\"47.5\" lon=\"11.5\"><ele>500</ele></trkpt>\n" +
                "      <trkpt lat=\"47.501\" lon=\"11.501\"><ele>510</ele></trkpt>\n" +
                "      <trkpt lat=\"47.502\" lon=\"11.502\"><ele>520</ele></trkpt>\n" +
                "    </trkseg>\n" +
                "  </trk>\n" +
                "</gpx>").getBytes(StandardCharsets.UTF_8);

        // GPX with significant elevation changes for section detection
        gpxWithElevationChanges = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<gpx version=\"1.1\" creator=\"Test\">\n" +
                "  <metadata><name>Mountain Track</name></metadata>\n" +
                "  <trk>\n" +
                "    <trkseg>\n" +
                // Flat start
                "      <trkpt lat=\"47.0\" lon=\"11.0\"><ele>1000</ele></trkpt>\n" +
                "      <trkpt lat=\"47.001\" lon=\"11.001\"><ele>1000</ele></trkpt>\n" +
                // Big climb (100m over ~100m distance)
                "      <trkpt lat=\"47.002\" lon=\"11.002\"><ele>1050</ele></trkpt>\n" +
                "      <trkpt lat=\"47.003\" lon=\"11.003\"><ele>1100</ele></trkpt>\n" +
                "      <trkpt lat=\"47.004\" lon=\"11.004\"><ele>1100</ele></trkpt>\n" +
                // Descent
                "      <trkpt lat=\"47.005\" lon=\"11.005\"><ele>1050</ele></trkpt>\n" +
                "      <trkpt lat=\"47.006\" lon=\"11.006\"><ele>1000</ele></trkpt>\n" +
                "    </trkseg>\n" +
                "  </trk>\n" +
                "</gpx>").getBytes(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Parse valid GPX file")
    void testParseValidGpx() throws Exception {
        GpxUploadController.GpxData data = GpxUploadController.parseGpxBytes(validGpxBytes);

        assertNotNull(data);
        assertEquals("Simple Track", data.getName());
        assertEquals(3, data.getPoints().size());
        assertEquals(47.5, data.getStartLat(), 0.001);
        assertEquals(11.5, data.getStartLon(), 0.001);
    }

    @Test
    @DisplayName("Calculate metrics correctly")
    void testMetricsCalculation() throws Exception {
        GpxUploadController.GpxData data = GpxUploadController.parseGpxBytes(validGpxBytes);

        assertTrue(data.getDistanceMeters() > 0, "Distance should be > 0");
        assertEquals(20, data.getElevationGainMeters(), 1.0, "Elevation gain should be ~20m");
        assertEquals(0, data.getElevationLossMeters(), 0.1, "No elevation loss expected");
        assertEquals(520, data.getHighestPoint(), 0.1);
        assertEquals(500, data.getLowestPoint(), 0.1);
    }

    @Test
    @DisplayName("Detect uphill and downhill sections")
    void testSectionDetection() throws Exception {
        GpxUploadController.GpxData data = GpxUploadController.parseGpxBytes(gpxWithElevationChanges);
        List sections = GpxUploadController.detectSections(data);

        // Should find at least an uphill and downhill section
        assertFalse(sections.isEmpty(), "Should detect sections");
    }

    @Test
    @DisplayName("Handle empty GPX gracefully")
    void testEmptyGpx() throws Exception {
        byte[] emptyGpx = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<gpx version=\"1.1\" creator=\"Test\">\n" +
                "  <metadata><name>Empty Track</name></metadata>\n" +
                "  <trk>\n" +
                "    <trkseg>\n" +
                "    </trkseg>\n" +
                "  </trk>\n" +
                "</gpx>").getBytes(StandardCharsets.UTF_8);

        GpxUploadController.GpxData data = GpxUploadController.parseGpxBytes(emptyGpx);

        assertNotNull(data);
        assertEquals("Empty Track", data.getName());
        assertEquals(0, data.getPoints().size());
    }

    @Test
    @DisplayName("Slice GPX data correctly")
    void testSliceGpxData() throws Exception {
        GpxUploadController.GpxData data = GpxUploadController.parseGpxBytes(validGpxBytes);
        GpxUploadController.GpxData sliced = data.slice(0, 1);

        assertEquals(2, sliced.getPoints().size());
        assertEquals(47.5, sliced.getStartLat(), 0.001);
    }

    @Test
    @DisplayName("Convert GPX to bytes maintains structure")
    void testGpxToBytes() throws Exception {
        GpxUploadController.GpxData data = GpxUploadController.parseGpxBytes(validGpxBytes);
        byte[] newBytes = data.toGpxBytes();

        GpxUploadController.GpxData reparsed = GpxUploadController.parseGpxBytes(newBytes);
        
        assertEquals(data.getPoints().size(), reparsed.getPoints().size());
        assertEquals(data.getStartLat(), reparsed.getStartLat(), 0.001);
    }

}
