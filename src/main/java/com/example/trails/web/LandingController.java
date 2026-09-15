package com.example.trails.web;

import com.example.trails.service.TrackService;
import com.example.trails.dto.TrackResponse;
import com.example.trails.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class LandingController {

    private final TrackService trackService;
    private final UserService userService;

    public LandingController(TrackService trackService, UserService userService) {
        this.trackService = trackService;
        this.userService = userService;
    }

    @GetMapping("/landing")
    public String landing(Model model, Principal principal) {
        List<TrackResponse> allTours = trackService.findVisibleSummaries(
                userService.getUserByEmail(principal.getName()));
        List<TrackResponse> tours = allTours.stream()
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

    @GetMapping("/tours")
    public String tours() {
        return "tours";
    }
}
