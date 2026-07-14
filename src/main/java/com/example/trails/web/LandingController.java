package com.example.trails.web;

import com.example.trails.service.TrackService;
import com.example.trails.dto.TrackResponse;
import com.example.trails.model.GPXTrack;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class LandingController {

    private final TrackService trackService;

    public LandingController(TrackService trackService) {
        this.trackService = trackService;
    }

    @GetMapping("/landing")
    public String landing(Model model) {
        List<GPXTrack> allTours = trackService.findAll();
        List<TrackResponse> tours = allTours.stream()
                .map(TrackResponse::new)
                .limit(6)
                .collect(Collectors.toList());
        model.addAttribute("featuredTours", tours);
        model.addAttribute("totalTours", allTours.size());
        return "landing";
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/landing";
    }
}
