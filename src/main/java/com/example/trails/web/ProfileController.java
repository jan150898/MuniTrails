package com.example.trails.web;

import com.example.trails.model.User;
import com.example.trails.service.UserService;
import com.example.trails.service.TrackService;
import com.example.trails.dto.TrackResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.security.Principal;
import java.util.List;

@Controller
public class ProfileController {

    private final UserService userService;
    private final TrackService trackService;

    public ProfileController(UserService userService, TrackService trackService) {
        this.userService = userService;
        this.trackService = trackService;
    }

    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        User user = userService.getUserByUsername(principal.getName());
        
        List<TrackResponse> tourResponses = trackService.findSummariesByCreatorId(user.getId());
        
        model.addAttribute("user", user);
        model.addAttribute("userTours", tourResponses);
        model.addAttribute("tourCount", tourResponses.size());
        model.addAttribute("totalDistance", 
            tourResponses.stream().mapToDouble(t -> t.getDistanceMeters() / 1000).sum());
        model.addAttribute("totalElevation",
            tourResponses.stream().mapToDouble(TrackResponse::getElevationGainMeters).sum());
        
        return "profile";
    }
}
