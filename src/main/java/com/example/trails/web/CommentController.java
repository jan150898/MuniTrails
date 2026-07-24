package com.example.trails.web;

import com.example.trails.model.Comment;
import com.example.trails.model.GPXTrack;
import com.example.trails.model.User;
import com.example.trails.repo.CommentRepository;
import com.example.trails.repo.GPXTrackRepository;
import com.example.trails.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {

    private static final Logger logger = LoggerFactory.getLogger(CommentController.class);
    
    private final CommentRepository commentRepository;
    private final GPXTrackRepository trackRepository;
    private final UserService userService;

    public CommentController(CommentRepository commentRepository, GPXTrackRepository trackRepository, UserService userService) {
        this.commentRepository = commentRepository;
        this.trackRepository = trackRepository;
        this.userService = userService;
    }

    /**
     * Get all comments for a track
     */
    @GetMapping("/track/{trackId}")
    public ResponseEntity<?> getComments(@PathVariable UUID trackId) {
        try {
            List<Comment> comments = commentRepository.findByTrack_IdOrderByCreatedAtDesc(trackId);
            var response = comments.stream().map(c -> Map.of(
                "id", c.getId().toString(),
                "text", c.getText(),
                "author", c.getUser().getUsername(),
                "createdAt", c.getCreatedAt().toString(),
                "updatedAt", c.getUpdatedAt().toString()
            )).toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching comments for track {}", trackId, e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch comments"));
        }
    }

    /**
     * Post a new comment on a track
     */
    @PostMapping("/track/{trackId}")
    public ResponseEntity<?> addComment(
            @PathVariable UUID trackId,
            @RequestBody Map<String, String> body,
            Principal principal) {
        try {
            String text = body.get("text");
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Comment text is required"));
            }
            if (text.length() > 4000) {
                return ResponseEntity.badRequest().body(Map.of("error", "Comment too long (max 4000 chars)"));
            }

            GPXTrack track = trackRepository.findById(trackId)
                    .orElseThrow(() -> new RuntimeException("Track not found"));
            User user = userService.getUserByUsername(principal.getName());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
            }

            Comment comment = new Comment();
            comment.setTrack(track);
            comment.setUser(user);
            comment.setText(text.trim());
            Comment saved = commentRepository.save(comment);

            logger.info("Comment posted on track {} by user {}", trackId, user.getUsername());

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", saved.getId().toString(),
                "text", saved.getText(),
                "author", saved.getUser().getUsername(),
                "createdAt", saved.getCreatedAt().toString()
            ));
        } catch (Exception e) {
            logger.error("Error posting comment", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to post comment: " + e.getMessage()));
        }
    }

    /**
     * Delete a comment (only author or admin can delete)
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable UUID commentId, Principal principal) {
        try {
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new RuntimeException("Comment not found"));
            
            User user = userService.getUserByUsername(principal.getName());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
            }

            // Only allow author or admin to delete
            if (!comment.getUser().getId().equals(user.getId()) && !user.getRole().equals("ROLE_ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Not authorized to delete this comment"));
            }

            commentRepository.deleteById(commentId);
            logger.info("Comment {} deleted by user {}", commentId, user.getUsername());

            return ResponseEntity.ok(Map.of("status", "deleted"));
        } catch (Exception e) {
            logger.error("Error deleting comment", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to delete comment"));
        }
    }
}
