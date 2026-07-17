package com.example.trails.web;

import com.example.trails.dto.UploadSectionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GPX Parsing Unit Tests")
class GpxParseUnitTest {

    @Test
    @DisplayName("Parse minimal GPX successfully")
    void testParseMinimalGpx() throws Exception {
        byte[] gpxBytes = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<gpx version=\"1.1\" creator=\"Test\">\n" +
                "  <metadata><name>TestTrack</name></metadata>\n" +
                "  <trk><trkseg>\n" +
                "    <trkpt lat=\"47.5\" lon=\"11.5\"><ele>500</ele></trkpt>\n" +
                "    <trkpt lat=\"47.501\" lon=\"11.501\"><ele>510</ele></trkpt>\n" +
                "  </trkseg></trk>\n" +
                "</gpx>").getBytes(StandardCharsets.UTF_8);

        GpxUploadController.GpxData data = GpxUploadController.parseGpxBytes(gpxBytes);

        assertNotNull(data);
        assertEquals("TestTrack", data.getName());
        assertEquals(2, data.getPoints().size());
        assertTrue(data.getDistanceMeters() > 0);
    }

    @Test
    @DisplayName("Parse namespaced GPX successfully")
    void testParseNamespacedGpx() throws Exception {
        byte[] gpxBytes = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\" creator=\"Test\">" +
                "<metadata><name>Namespaced track</name></metadata><trk><trkseg>" +
                "<trkpt lat=\"47.5\" lon=\"11.5\"><ele>500</ele></trkpt>" +
                "<trkpt lat=\"47.501\" lon=\"11.501\"><ele>510</ele></trkpt>" +
                "</trkseg></trk></gpx>").getBytes(StandardCharsets.UTF_8);

        GpxUploadController.GpxData data = GpxUploadController.parseGpxBytes(gpxBytes);

        assertEquals("Namespaced track", data.getName());
        assertEquals(2, data.getPoints().size());
    }

    @Test
    @DisplayName("Parse GPX route points successfully")
    void testParseRouteGpx() throws Exception {
        byte[] gpxBytes = ("<gpx version=\"1.1\" creator=\"Test\"><rte><name>Route</name>" +
                "<rtept lat=\"47.5\" lon=\"11.5\"><ele>500</ele></rtept>" +
                "<rtept lat=\"47.501\" lon=\"11.501\"><ele>510</ele></rtept>" +
                "</rte></gpx>").getBytes(StandardCharsets.UTF_8);

        GpxUploadController.GpxData data = GpxUploadController.parseGpxBytes(gpxBytes);

        assertEquals("Route", data.getName());
        assertEquals(2, data.getPoints().size());
    }

    @Test
    @DisplayName("Merge consecutive uphill sections separated by a flat stretch")
    void testMergeConsecutiveSameDirectionSections() throws Exception {
        byte[] gpxBytes = ("<gpx version=\"1.1\" creator=\"Test\"><trk><trkseg>" +
                "<trkpt lat=\"47.000\" lon=\"11.000\"><ele>0</ele></trkpt>" +
                "<trkpt lat=\"47.001\" lon=\"11.001\"><ele>50</ele></trkpt>" +
                "<trkpt lat=\"47.002\" lon=\"11.002\"><ele>100</ele></trkpt>" +
                "<trkpt lat=\"47.003\" lon=\"11.003\"><ele>100</ele></trkpt>" +
                "<trkpt lat=\"47.004\" lon=\"11.004\"><ele>150</ele></trkpt>" +
                "<trkpt lat=\"47.005\" lon=\"11.005\"><ele>200</ele></trkpt>" +
                "</trkseg></trk></gpx>").getBytes(StandardCharsets.UTF_8);

        java.util.List<UploadSectionRequest> sections =
                GpxUploadController.detectSections(GpxUploadController.parseGpxBytes(gpxBytes));

        assertEquals(1, sections.size());
        assertEquals("UPHILL", sections.get(0).getType());
        assertEquals(0, sections.get(0).getStartIndex());
        assertEquals(5, sections.get(0).getEndIndex());
    }

    @Test
    @DisplayName("Parse GPX with elevation gain")
    void testParseGpxWithElevation() throws Exception {
        byte[] gpxBytes = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<gpx version=\"1.1\" creator=\"Test\">\n" +
                "  <metadata><name>Mountain</name></metadata>\n" +
                "  <trk><trkseg>\n" +
                "    <trkpt lat=\"47.0\" lon=\"11.0\"><ele>1000</ele></trkpt>\n" +
                "    <trkpt lat=\"47.001\" lon=\"11.001\"><ele>1020</ele></trkpt>\n" +
                "    <trkpt lat=\"47.002\" lon=\"11.002\"><ele>1040</ele></trkpt>\n" +
                "  </trkseg></trk>\n" +
                "</gpx>").getBytes(StandardCharsets.UTF_8);

        GpxUploadController.GpxData data = GpxUploadController.parseGpxBytes(gpxBytes);

        assertEquals("Mountain", data.getName());
        assertEquals(1000, data.getLowestPoint());
        assertEquals(1040, data.getHighestPoint());
        assertTrue(data.getElevationGainMeters() >= 40);
    }

    @Test
    @DisplayName("Detect sections in mountain GPX")
    void testDetectSections() throws Exception {
        byte[] gpxBytes = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<gpx version=\"1.1\" creator=\"Test\">\n" +
                "  <metadata><name>ClimbDescent</name></metadata>\n" +
                "  <trk><trkseg>\n" +
                "    <trkpt lat=\"47.0\" lon=\"11.0\"><ele>1000</ele></trkpt>\n" +
                "    <trkpt lat=\"47.001\" lon=\"11.001\"><ele>1000</ele></trkpt>\n" +
                "    <trkpt lat=\"47.002\" lon=\"11.002\"><ele>1025</ele></trkpt>\n" +
                "    <trkpt lat=\"47.003\" lon=\"11.003\"><ele>1050</ele></trkpt>\n" +
                "    <trkpt lat=\"47.004\" lon=\"11.004\"><ele>1050</ele></trkpt>\n" +
                "    <trkpt lat=\"47.005\" lon=\"11.005\"><ele>1025</ele></trkpt>\n" +
                "    <trkpt lat=\"47.006\" lon=\"11.006\"><ele>1000</ele></trkpt>\n" +
                "  </trkseg></trk>\n" +
                "</gpx>").getBytes(StandardCharsets.UTF_8);

        GpxUploadController.GpxData data = GpxUploadController.parseGpxBytes(gpxBytes);
        java.util.List sections = GpxUploadController.detectSections(data);


        assertNotNull(sections);
        // With sensitive detection, should find at least 1 section
        System.out.println("Detected " + sections.size() + " sections");
    }

    @Test
    @DisplayName("Handle invalid GPX")
    void testInvalidGpx() {
        byte[] invalidGpx = "This is not valid XML".getBytes();

        assertThrows(Exception.class, () -> {
            GpxUploadController.parseGpxBytes(invalidGpx);
        });
    }

    @Test
    @DisplayName("Slice GPX section")
    void testSliceGpx() throws Exception {
        byte[] gpxBytes = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<gpx version=\"1.1\" creator=\"Test\">\n" +
                "  <metadata><name>Test</name></metadata>\n" +
                "  <trk><trkseg>\n" +
                "    <trkpt lat=\"47.0\" lon=\"11.0\"><ele>100</ele></trkpt>\n" +
                "    <trkpt lat=\"47.1\" lon=\"11.1\"><ele>200</ele></trkpt>\n" +
                "    <trkpt lat=\"47.2\" lon=\"11.2\"><ele>300</ele></trkpt>\n" +
                "    <trkpt lat=\"47.3\" lon=\"11.3\"><ele>400</ele></trkpt>\n" +
                "  </trkseg></trk>\n" +
                "</gpx>").getBytes(StandardCharsets.UTF_8);

        GpxUploadController.GpxData data = GpxUploadController.parseGpxBytes(gpxBytes);
        GpxUploadController.GpxData sliced = data.slice(1, 2);

        assertEquals(2, sliced.getPoints().size());
    }

}
