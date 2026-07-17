package com.example.trails.web;

import com.example.trails.dto.TrackResponse;
import com.example.trails.service.TrackService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Controller
public class TourPageController {
    private final TrackService trackService;

    public TourPageController(TrackService trackService) {
        this.trackService = trackService;
    }

    @GetMapping("/tour/{trackId}")
    public String tour(@PathVariable UUID trackId, Model model) {
        TrackResponse tour = trackService.findSummaryById(trackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tour not found"));
        model.addAttribute("tour", tour);
        return "tour";
    }
}
