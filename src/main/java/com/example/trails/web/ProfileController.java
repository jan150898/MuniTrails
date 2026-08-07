package com.example.trails.web;

import com.example.trails.model.User;
import com.example.trails.service.UserService;
import com.example.trails.service.TrackService;
import com.example.trails.dto.TrackResponse;
import com.example.trails.dto.ChangePasswordRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
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

    @GetMapping("/change-password")
    public String changePasswordForm(Model model) {
        model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        return "change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(Principal principal, ChangePasswordRequest request, RedirectAttributes redirectAttributes) {
        if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "New password cannot be empty");
            return "redirect:/change-password";
        }
        
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match");
            return "redirect:/change-password";
        }
        
        try {
            userService.changePassword(principal.getName(), request.getCurrentPassword(), request.getNewPassword());
            redirectAttributes.addFlashAttribute("success", "Password changed successfully");
            return "redirect:/profile";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/change-password";
        }
    }
}
