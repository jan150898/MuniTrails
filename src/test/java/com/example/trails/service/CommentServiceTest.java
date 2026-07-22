package com.example.trails.service;

import com.example.trails.model.Comment;
import com.example.trails.model.GPXTrack;
import com.example.trails.model.GPXTrackType;
import com.example.trails.model.User;
import com.example.trails.repo.CommentRepository;
import com.example.trails.repo.GPXTrackRepository;
import com.example.trails.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CommentService
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Comment Service Tests")
class CommentServiceTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TrackService trackService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GPXTrackRepository gpxTrackRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private User otherUser;
    private GPXTrack testTrack;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        gpxTrackRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("comment_user");
        testUser.setPasswordHash(passwordEncoder.encode("password"));
        testUser.setRole("ROLE_USER");
        testUser = userRepository.save(testUser);

        otherUser = new User();
        otherUser.setUsername("other_user");
        otherUser.setPasswordHash(passwordEncoder.encode("password"));
        otherUser.setRole("ROLE_USER");
        otherUser = userRepository.save(otherUser);

        testTrack = trackService.createTrack("Test Track", GPXTrackType.TOUR, 47.5, 11.5, 1000, 100, 50, testUser);
    }

    @Test
    @DisplayName("Create comment successfully")
    void testCreateComment() {
        Comment comment = commentService.createComment(testTrack, testUser, "Great trail!");

        assertNotNull(comment.getId(), "Comment should have ID");
        assertEquals("Great trail!", comment.getText());
        assertEquals(testTrack.getId(), comment.getTrack().getId());
        assertEquals(testUser.getId(), comment.getUser().getId());
        assertNotNull(comment.getCreatedAt(), "Comment should have timestamp");
    }

    @Test
    @DisplayName("Find comments by track ID")
    void testFindByTrackId() {
        commentService.createComment(testTrack, testUser, "Comment 1");
        commentService.createComment(testTrack, otherUser, "Comment 2");
        commentService.createComment(testTrack, testUser, "Comment 3");

        List<Comment> comments = commentService.findByTrackId(testTrack.getId());

        assertEquals(3, comments.size());
        assertTrue(comments.stream().allMatch(c -> c.getTrack().getId().equals(testTrack.getId())));
    }

    @Test
    @DisplayName("Find comments by track ID returns empty for track with no comments")
    void testFindByTrackIdEmpty() {
        List<Comment> comments = commentService.findByTrackId(UUID.randomUUID());

        assertTrue(comments.isEmpty());
    }

    @Test
    @DisplayName("Delete comment")
    void testDeleteComment() {
        Comment comment = commentService.createComment(testTrack, testUser, "To delete");
        UUID commentId = comment.getId();

        commentService.deleteComment(commentId);

        List<Comment> comments = commentService.findByTrackId(testTrack.getId());
        assertFalse(comments.stream().anyMatch(c -> c.getId().equals(commentId)), "Comment should be deleted");
    }

    @Test
    @DisplayName("Create comment with long text")
    void testCreateCommentWithLongText() {
        String longText = "A".repeat(1000);
        Comment comment = commentService.createComment(testTrack, testUser, longText);

        assertEquals(longText, comment.getText());
    }

    @Test
    @DisplayName("Create comment with special characters")
    void testCreateCommentWithSpecialCharacters() {
        String text = "Great trail! 🏔️ Awesome 😎 @user #mountains";
        Comment comment = commentService.createComment(testTrack, testUser, text);

        assertEquals(text, comment.getText());
    }

    @Test
    @DisplayName("Comment text can be minimal")
    void testCommentMinimalText() {
        Comment comment = commentService.createComment(testTrack, testUser, ".");

        assertEquals(".", comment.getText());
    }

    @Test
    @DisplayName("Multiple tracks can have comments from same user")
    void testMultipleTracksComments() {
        GPXTrack track2 = trackService.createTrack("Track 2", GPXTrackType.TOUR, 47.6, 11.6, 2000, 200, 100, otherUser);

        commentService.createComment(testTrack, testUser, "Comment on track 1");
        commentService.createComment(track2, testUser, "Comment on track 2");

        List<Comment> track1Comments = commentService.findByTrackId(testTrack.getId());
        List<Comment> track2Comments = commentService.findByTrackId(track2.getId());

        assertEquals(1, track1Comments.size());
        assertEquals(1, track2Comments.size());
    }

    @Test
    @DisplayName("Comment timestamps are set")
    void testCommentTimestamps() {
        Comment comment = commentService.createComment(testTrack, testUser, "Timestamped");

        assertNotNull(comment.getCreatedAt());
    }
}
