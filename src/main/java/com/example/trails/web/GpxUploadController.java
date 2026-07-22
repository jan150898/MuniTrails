package com.example.trails.web;

import com.example.trails.dto.TrackResponse;
import com.example.trails.dto.UploadSectionRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.trails.model.GPXTrack;
import com.example.trails.model.GPXTrackStatus;
import com.example.trails.model.GPXTrackType;
import com.example.trails.model.User;
import com.example.trails.model.Visibility;
import com.example.trails.repo.GPXTrackRepository;
import com.example.trails.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class GpxUploadController {

    private final GPXTrackRepository gpxTrackRepository;
    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GpxUploadController(GPXTrackRepository gpxTrackRepository, UserService userService) {
        this.gpxTrackRepository = gpxTrackRepository;
        this.userService = userService;
    }

    @PostMapping("/tracks/upload-gpx")
    public ResponseEntity<?> uploadGpx(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("type") String type,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "sections", required = false) String sectionsJson,
            @RequestParam(value = "difficultyMin", required = false) String difficultyMin,
            @RequestParam(value = "difficultyMax", required = false) String difficultyMax,
            Principal principal) {
        
        try {
            // Validate inputs
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Please select a non-empty GPX file."));
            }
            if (type == null || type.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Please select a tour type."));
            }
            
            User user = userService.getUserByUsername(principal.getName());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Please sign in again before uploading."));
            }
            
            // Parse GPX file
            GpxData gpxData = parseGpxFile(file);
            byte[] bytes = file.getBytes();
            String trackName = name == null || name.trim().isEmpty() ? gpxData.getName() : name.trim();
            if (trackName == null || trackName.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Add a tour name or use a GPX file that contains one."));
            }
            if (gpxData.getPoints().size() < 2) {
                return ResponseEntity.badRequest().body(Map.of("error", "The GPX file must contain at least two track or route points."));
            }

            // Parse sections first so we can derive tour-level evaluation values
            List<UploadSectionRequest> parsedSections = readSections(sectionsJson);

            List<UploadSectionRequest> includedSections = parsedSections.stream()
                    .filter(UploadSectionRequest::isIncluded)
                    .filter(s -> s.getType() != null && ("UPHILL".equalsIgnoreCase(s.getType()) || "DOWNHILL".equalsIgnoreCase(s.getType())))
                    .sorted((a, b) -> Integer.compare(a.getStartIndex(), b.getStartIndex()))
                    .collect(java.util.stream.Collectors.toList());

            // Normalize section boundaries:
            // - An UPHILL must end right where the next DOWNHILL starts (or tour end).
            // - A DOWNHILL must end right where the next UPHILL starts (or tour end).
            // - Neutral/gaps are allowed only between DOWNHILL and UPHILL (not forced to adjacency).
            //
            // We do this by adjusting start/end indices based on the *type order* (UPHILL <-> DOWNHILL).
            // The client may upload multiple sections of same type (e.g. "Uphill splitted in two");
            // we merge them by chaining their indices into a single continuous block.
            List<UploadSectionRequest> normalizedSections = normalizeUphillDownhillSections(includedSections, gpxData.getPoints().size() - 1);


            // Tour-level evaluation: computed from normalized UPHILL/DOWNHILL sections.
            // overallRating + exposition: average (rounded)
            // uphillRating: minimum (worse one)
            int computedOverallRating = 0;
            int computedExposition = 5;
            int computedUphillRating = 5;
            if (!normalizedSections.isEmpty()) {
                double avgOverall = normalizedSections.stream().mapToInt(UploadSectionRequest::getOverallRating).average().orElse(0);
                double avgExposition = normalizedSections.stream().mapToInt(UploadSectionRequest::getExposition).average().orElse(5);
                int minUphill = normalizedSections.stream().mapToInt(UploadSectionRequest::getUphillRating).min().orElse(5);

                computedOverallRating = (int) Math.round(avgOverall);
                computedExposition = (int) Math.round(avgExposition);
                computedUphillRating = minUphill;
            }

            // rideAgain: if any section says true, mark tour rideAgain=true
            boolean computedRideAgain = !normalizedSections.isEmpty() && normalizedSections.stream().anyMatch(UploadSectionRequest::isRideAgain);

            GPXTrack savedTrack = saveTrack(trackName,
                    type, gpxData, bytes, user,
                    computedOverallRating, computedExposition, computedUphillRating, computedRideAgain);
            if (applyDifficulty(savedTrack, type, difficultyMin, difficultyMax)) {
                savedTrack = gpxTrackRepository.save(savedTrack);
            }

            // Save normalized sections (only included ones)
            for (UploadSectionRequest section : normalizedSections) {
                GpxData sectionData = gpxData.slice(section.getStartIndex(), section.getEndIndex());
                if (sectionData.getPoints().size() < 2) continue;
                String sectionType = section.getType();
                String sectionName = section.getName() == null || section.getName().isBlank()
                        ? savedTrack.getName() + " – " + sectionType.toLowerCase()
                        : section.getName();
                saveTrack(sectionName, sectionType, sectionData, sectionData.toGpxBytes(), user,
                        section.getOverallRating(), section.getExposition(), section.getUphillRating(), section.isRideAgain());
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(new TrackResponse(savedTrack));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Could not save this GPX file: " + safeMessage(e)));
        }
    }

    @PostMapping("/tracks/analyze-gpx")
    public ResponseEntity<?> analyzeGpx(@RequestParam("file") MultipartFile file) {
        try {
            GpxData data = parseGpxFile(file);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("name", data.getName());
            result.put("points", mapPoints(data));
            result.put("sections", SectionDetector.detectSections(data.getPoints()));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Could not read GPX: " + e.getMessage()));
        }
    }

    private List<UploadSectionRequest> readSections(String sectionsJson) throws IOException {
        return sectionsJson == null || sectionsJson.trim().isEmpty() ? List.of()
                : objectMapper.readValue(sectionsJson, new TypeReference<List<UploadSectionRequest>>() {});
    }

    private static List<UploadSectionRequest> normalizeUphillDownhillSections(List<UploadSectionRequest> includedSections, int tourLastIndex) {
        if (includedSections == null || includedSections.isEmpty()) return List.of();

        // Keep only valid types and sort by start.
        List<UploadSectionRequest> sections = includedSections.stream()
                .filter(s -> s != null && s.getType() != null)
                .filter(s -> "UPHILL".equalsIgnoreCase(s.getType()) || "DOWNHILL".equalsIgnoreCase(s.getType()))
                    .sorted((a, b) -> Integer.compare(a.getStartIndex(), b.getStartIndex()))
                    .collect(java.util.stream.Collectors.toList());

        List<UploadSectionRequest> result = new ArrayList<>();
        UploadSectionRequest currentBlock = null;

        // Coalesce consecutive sections with the same type into a single continuous block.
        // This prevents "UPHILL split into two" from creating extra boundaries.
        for (UploadSectionRequest s : sections) {
            int start = clampIndex(s.getStartIndex(), 0, tourLastIndex);
            int end = clampIndex(s.getEndIndex(), 0, tourLastIndex);
            if (end <= start) continue;

            String type = s.getType();

            if (currentBlock == null) {
                currentBlock = copySectionForNormalization(s, start, end);
                continue;
            }

            String currentType = currentBlock.getType();
            int currentEnd = currentBlock.getEndIndex();

            if (equalsType(currentType, type)) {
                // A neutral stretch between two sections in the same direction
                // still belongs to that climb/descent. Keep it as one track.
                currentBlock.setEndIndex(Math.max(currentEnd, end));
                // keep existing ratings/name; GPX range is the important part
            } else {
                result.add(currentBlock);
                currentBlock = copySectionForNormalization(s, start, end);
            }
        }
        if (currentBlock != null) result.add(currentBlock);

        if (result.isEmpty()) return List.of();

        // Enforce adjacency between UPHILL <-> DOWNHILL transitions by rewriting indices.
        // Rule:
        // - An UPHILL section ends at the next DOWNHILL start (or tour end).
        // - A DOWNHILL section ends at the next UPHILL start (or tour end).
        // gaps are allowed only between DH and UH (we do NOT force those to be contiguous).
        // Implementation: if we have ... UPHILL, DOWNHILL ... then set UPHILL.end = DOWNHILL.start.
        for (int i = 0; i < result.size() - 1; i++) {
            UploadSectionRequest a = result.get(i);
            UploadSectionRequest b = result.get(i + 1);
            String typeA = a.getType();
            String typeB = b.getType();
            if (!isUphillDownhillPair(typeA, typeB)) continue;

            if (isUphill(typeA) && isDownhill(typeB)) {
                a.setEndIndex(clampIndex(b.getStartIndex(), 0, tourLastIndex));
            } else if (isDownhill(typeA) && isUphill(typeB)) {
                a.setEndIndex(clampIndex(b.getStartIndex(), 0, tourLastIndex));
            }
        }

        // Also ensure last section ends at tour end.
        UploadSectionRequest last = result.get(result.size() - 1);
        last.setEndIndex(clampIndex(last.getEndIndex(), 0, tourLastIndex));

        // Fix any invalid ranges that might have been created.
        List<UploadSectionRequest> cleaned = new ArrayList<>();
        for (UploadSectionRequest s : result) {
            int start = clampIndex(s.getStartIndex(), 0, tourLastIndex);
            int end = clampIndex(s.getEndIndex(), 0, tourLastIndex);
            if (end <= start) continue;
            s.setStartIndex(start);
            s.setEndIndex(end);
            cleaned.add(s);
        }

        return cleaned;
    }

    private static UploadSectionRequest copySectionForNormalization(UploadSectionRequest src, int start, int end) {
        UploadSectionRequest c = new UploadSectionRequest();
        c.setStartIndex(start);
        c.setEndIndex(end);
        c.setType(src.getType());
        c.setName(src.getName());
        c.setIncluded(src.isIncluded());
        c.setOverallRating(src.getOverallRating());
        c.setExposition(src.getExposition());
        c.setUphillRating(src.getUphillRating());
        c.setRideAgain(src.isRideAgain());
        return c;
    }

    private static boolean equalsType(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    private static boolean isUphill(String t) {
        return t != null && "UPHILL".equalsIgnoreCase(t);
    }

    private static boolean isDownhill(String t) {
        return t != null && "DOWNHILL".equalsIgnoreCase(t);
    }

    private static boolean isUphillDownhillPair(String a, String b) {
        return (isUphill(a) && isDownhill(b)) || (isDownhill(a) && isUphill(b));
    }

    private static int clampIndex(int idx, int min, int max) {
        return Math.max(min, Math.min(max, idx));
    }


    private GPXTrack saveTrack(String name, String type, GpxData data, byte[] bytes, User user,
                               int overallRating, int exposition, int uphillRating, boolean rideAgain) {
        GPXTrack track = new GPXTrack();
        track.setName(name);
        track.setType(GPXTrackType.valueOf(type.toUpperCase()));
        track.setStatus(GPXTrackStatus.DRAFT);
        track.setVisibility(Visibility.PUBLIC);
        track.setCreatedBy(user);
        track.setLastEditedBy(user);
        if (data.getStartLat() != null) { track.setStartLat(data.getStartLat()); track.setStartLon(data.getStartLon()); }
        track.setDistanceMeters(data.getDistanceMeters());
        track.setElevationGainMeters(data.getElevationGainMeters());
        track.setElevationLossMeters(data.getElevationLossMeters());
        track.setHighestPointAltitudeMeters(data.getHighestPoint());
        track.setLowestPointAltitudeMeters(data.getLowestPoint());
        track.setGpxFile(bytes);
        track.setGpxFileChecksum(calculateChecksum(bytes));
        track.setOverallRating(overallRating);
        track.setExposition(exposition);
        track.setUphillRating(uphillRating);
        track.setRideAgain(rideAgain);
        return gpxTrackRepository.save(track);
    }

    // Package-visible static so GarminController can reuse the same logic
    static GpxData parseGpxBytes(byte[] bytes) throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        disableExternalEntities(factory);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new java.io.ByteArrayInputStream(bytes));
        GpxData data = new GpxData();
        NodeList metadataNodes = elementsByLocalName(doc, "metadata");
        if (metadataNodes.getLength() > 0) {
            NodeList nameNodes = metadataNodes.item(0).getChildNodes();
            for (int i = 0; i < nameNodes.getLength(); i++) {
                Node node = nameNodes.item(i);
                if ("name".equals(localName(node)) && node.getTextContent() != null && !node.getTextContent().isBlank()) {
                    data.setName(node.getTextContent().trim());
                }
            }
        }
        if ("Unnamed Track".equals(data.getName())) {
            NodeList trackNames = elementsByLocalName(doc, "name");
            for (int i = 0; i < trackNames.getLength(); i++) {
                Node node = trackNames.item(i);
                if (node.getTextContent() != null && !node.getTextContent().isBlank()) {
                    data.setName(node.getTextContent().trim());
                    break;
                }
            }
        }
        List<TrackPoint> trackPoints = new ArrayList<>();
        NodeList tpNodes = elementsByLocalName(doc, "trkpt");
        if (tpNodes.getLength() == 0) {
            tpNodes = elementsByLocalName(doc, "rtept");
        }
        for (int i = 0; i < tpNodes.getLength(); i++) {
            Node trkPt = tpNodes.item(i);
            Node latAttribute = trkPt.getAttributes().getNamedItem("lat");
            Node lonAttribute = trkPt.getAttributes().getNamedItem("lon");
            if (latAttribute == null || lonAttribute == null) {
                throw new IOException("A track point is missing latitude or longitude.");
            }
            String lat = latAttribute.getTextContent();
            String lon = lonAttribute.getTextContent();
            double elevation = 0;
            NodeList children = trkPt.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                if ("ele".equals(localName(child)) && child.getTextContent() != null) {
                    elevation = Double.parseDouble(child.getTextContent());
                }
            }
            trackPoints.add(new TrackPoint(Double.parseDouble(lat), Double.parseDouble(lon), elevation));
        }
        if (!trackPoints.isEmpty()) {
            data.getPoints().addAll(trackPoints);
            data.setStartLat(trackPoints.get(0).getLat());
            data.setStartLon(trackPoints.get(0).getLon());
            calculateMetrics(trackPoints, data);
        }
        return data;
    }

    private static NodeList elementsByLocalName(Document doc, String name) {
        NodeList namespaced = doc.getElementsByTagNameNS("*", name);
        return namespaced.getLength() > 0 ? namespaced : doc.getElementsByTagName(name);
    }

    private static String localName(Node node) {
        return node.getLocalName() == null ? node.getNodeName() : node.getLocalName();
    }

    private static void disableExternalEntities(DocumentBuilderFactory factory) throws ParserConfigurationException {
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
    }

    private static String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? "invalid GPX data" : message;
    }

    static boolean applyDifficulty(GPXTrack track, String type, String minimum, String maximum) {
        if (!"TOUR".equalsIgnoreCase(type) && !"DOWNHILL".equalsIgnoreCase(type)) return false;
        String min = difficulty(minimum);
        String max = difficulty(maximum);
        if (min == null && max == null) return false;
        if (min == null) min = max;
        if (max == null) max = min;
        if (min.compareTo(max) > 0) throw new IllegalArgumentException("Minimum difficulty cannot exceed maximum difficulty.");
        track.setDifficultyMin(min);
        track.setDifficultyMax(max);
        return true;
    }

    private static String difficulty(String value) {
        if (value == null || value.isBlank()) return null;
        String result = value.trim().toUpperCase();
        if (!result.matches("S[0-5]")) throw new IllegalArgumentException("Difficulty must be S0 through S5.");
        return result;
    }

    private GpxData parseGpxFile(MultipartFile file) throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return parseGpxBytes(file.getBytes());
    }

    private static void calculateMetrics(List<TrackPoint> points, GpxData data) {
        if (points.isEmpty()) return;
        
        double totalDistance = 0;
        double elevationGain = 0;
        double elevationLoss = 0;
        double highestPoint = points.get(0).getElevation();
        double lowestPoint = points.get(0).getElevation();
        
        for (int i = 1; i < points.size(); i++) {
            TrackPoint prev = points.get(i - 1);
            TrackPoint curr = points.get(i);
            
            // Calculate distance using Haversine
            totalDistance += haversineDistance(prev.getLat(), prev.getLon(), curr.getLat(), curr.getLon());
            
            // Calculate elevation changes
            double elevDiff = curr.getElevation() - prev.getElevation();
            if (elevDiff > 0) {
                elevationGain += elevDiff;
            } else {
                elevationLoss += Math.abs(elevDiff);
            }
            
            // Track highest/lowest
            highestPoint = Math.max(highestPoint, curr.getElevation());
            lowestPoint = Math.min(lowestPoint, curr.getElevation());
        }
        
        data.setDistanceMeters(totalDistance * 1000); // Convert km to m
        data.setElevationGainMeters(Math.round(elevationGain));
        data.setElevationLossMeters(Math.round(elevationLoss));
        data.setHighestPoint(highestPoint);
        data.setLowestPoint(lowestPoint);
    }

    /**
     * Finds sustained elevation changes.
     * Small fluctuations are deliberately ignored.
     *
     * Ordering rule (requested):
     * - An uphill section ends when a downhill starts (or at tour end).
     * - A downhill section ends when an uphill starts (or at tour end).
     * - Between DH and UH we may have "nothing" (neutral/no direction) segments.
     */
    static List<UploadSectionRequest> detectSections(GpxData data) {
        List<UploadSectionRequest> sections = new ArrayList<>();
        List<TrackPoint> points = data.getPoints();

        // state: current direction we are in
        //  1 => uphill, -1 => downhill, 0 => neutral/nothing
        int currentDirection = 0;
        int currentStartIndex = -1;

        for (int i = 1; i < points.size(); i++) {
            TrackPoint prev = points.get(i - 1);
            TrackPoint curr = points.get(i);
            double change = curr.getElevation() - prev.getElevation();

            // Make direction threshold dependent on distance between the two points.
            // This reduces false positives for very short segments (GPS noise) and
            // reacts appropriately on longer segments where real climbs/descents occur.
            double distanceMeters = haversineDistance(prev.getLat(), prev.getLon(), curr.getLat(), curr.getLon()) * 1000.0;
            // More sensitive: 0.3m base + 1.0x factor (was 0.5m + 1.5x), catches smaller changes
            double metersThreshold = 0.3 + (distanceMeters / 1000.0) * 1.0;

            int nextDirection = change > metersThreshold ? 1 : change < -metersThreshold ? -1 : 0;

            // Neutral => close any active section and go to NONE.
            if (nextDirection == 0) {
                if (currentDirection != 0) {
                    addSectionIfMeaningful(sections, points, currentStartIndex, i - 1, currentDirection);
                    currentDirection = 0;
                    currentStartIndex = -1;
                }
                continue;
            }

            // Non-neutral:
            // If we were neutral, start a new section.
            if (currentDirection == 0) {
                currentDirection = nextDirection;
                currentStartIndex = i - 1;
                continue;
            }

            // If direction changes, close the previous section and start the new one.
            if (currentDirection != nextDirection) {
                addSectionIfMeaningful(sections, points, currentStartIndex, i - 1, currentDirection);
                currentDirection = nextDirection;
                currentStartIndex = i - 1;
            }
        }

        // Close active section at end of tour.
        if (currentDirection != 0) {
            addSectionIfMeaningful(sections, points, currentStartIndex, points.size() - 1, currentDirection);
        }

        return mergeConsecutiveSameDirectionSections(sections);
    }

    /**
     * Detection closes a section when elevation briefly levels out.  If the next
     * meaningful section has the same direction, include that flat interval in
     * the same uphill/downhill instead of presenting two consecutive sections.
     */
    private static List<UploadSectionRequest> mergeConsecutiveSameDirectionSections(List<UploadSectionRequest> sections) {
        List<UploadSectionRequest> merged = new ArrayList<>();
        for (UploadSectionRequest section : sections) {
            if (!merged.isEmpty() && equalsType(merged.get(merged.size() - 1).getType(), section.getType())) {
                UploadSectionRequest previous = merged.get(merged.size() - 1);
                previous.setEndIndex(Math.max(previous.getEndIndex(), section.getEndIndex()));
            } else {
                merged.add(section);
            }
        }
        return merged;
    }

    static List<Map<String, Double>> mapPoints(GpxData data) {
        List<Map<String, Double>> result = new ArrayList<>();
        for (TrackPoint point : data.getPoints()) {
            result.add(Map.of("lat", point.getLat(), "lon", point.getLon(), "elevation", point.getElevation()));
        }
        return result;
    }

    private static void addSectionIfMeaningful(List<UploadSectionRequest> sections, List<TrackPoint> points,
                                                int start, int end, int direction) {
        if (end <= start) return;
        double elevation = points.get(end).getElevation() - points.get(start).getElevation();
        double distance = 0;
        for (int i = start + 1; i <= end; i++) distance += haversineDistance(
                points.get(i - 1).getLat(), points.get(i - 1).getLon(), points.get(i).getLat(), points.get(i).getLon()) * 1000;
        // More sensitive thresholds: 50m elevation over 150m distance (was 100m over 250m)
        if (Math.abs(elevation) < 50 || distance < 150) return;
        UploadSectionRequest section = new UploadSectionRequest();
        section.setStartIndex(start);
        section.setEndIndex(end);
        section.setType(direction > 0 ? "UPHILL" : "DOWNHILL");
        section.setName((direction > 0 ? "Uphill" : "Downhill") + " " + (sections.size() + 1));
        sections.add(section);
    }

    private static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private String calculateChecksum(byte[] data) {
        // Simple checksum for now
        long checksum = 0;
        for (byte b : data) {
            checksum += b;
        }
        return String.format("%x", checksum);
    }

    // Helper classes
    static class GpxData {
        private String name = "Unnamed Track";
        private Double startLat;
        private Double startLon;
        private double distanceMeters = 0;
        private double elevationGainMeters = 0;
        private double elevationLossMeters = 0;
        private double highestPoint = 0;
        private double lowestPoint = 0;
        private final List<TrackPoint> points = new ArrayList<>();

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Double getStartLat() { return startLat; }
        public void setStartLat(Double startLat) { this.startLat = startLat; }
        public Double getStartLon() { return startLon; }
        public void setStartLon(Double startLon) { this.startLon = startLon; }
        public double getDistanceMeters() { return distanceMeters; }
        public void setDistanceMeters(double distanceMeters) { this.distanceMeters = distanceMeters; }
        public double getElevationGainMeters() { return elevationGainMeters; }
        public void setElevationGainMeters(double elevationGainMeters) { this.elevationGainMeters = elevationGainMeters; }
        public double getElevationLossMeters() { return elevationLossMeters; }
        public void setElevationLossMeters(double elevationLossMeters) { this.elevationLossMeters = elevationLossMeters; }
        public double getHighestPoint() { return highestPoint; }
        public void setHighestPoint(double highestPoint) { this.highestPoint = highestPoint; }
        public double getLowestPoint() { return lowestPoint; }
        public void setLowestPoint(double lowestPoint) { this.lowestPoint = lowestPoint; }
        public List<TrackPoint> getPoints() { return points; }

        GpxData slice(int start, int end) {
            GpxData result = new GpxData();
            int first = Math.max(0, start), last = Math.min(points.size() - 1, end);
            if (first > last) return result;
            result.points.addAll(points.subList(first, last + 1));
            TrackPoint startPoint = result.points.get(0);
            result.setStartLat(startPoint.getLat());
            result.setStartLon(startPoint.getLon());
            calculateMetrics(result.points, result);
            return result;
        }

        byte[] toGpxBytes() {
            StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><gpx version=\"1.1\" creator=\"Muni Trails\"><trk><trkseg>");
            for (TrackPoint point : points) xml.append("<trkpt lat=\"").append(point.getLat()).append("\" lon=\"")
                    .append(point.getLon()).append("\"><ele>").append(point.getElevation()).append("</ele></trkpt>");
            return (xml.append("</trkseg></trk></gpx>")).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    static class TrackPoint {
        private double lat;
        private double lon;
        private double elevation;

        TrackPoint(double lat, double lon, double elevation) {
            this.lat = lat;
            this.lon = lon;
            this.elevation = elevation;
        }

        double getLat() { return lat; }
        double getLon() { return lon; }
        double getElevation() { return elevation; }
    }
}
