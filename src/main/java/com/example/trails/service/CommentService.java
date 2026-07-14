package com.example.trails.service;

import com.example.trails.model.Comment;
import com.example.trails.model.GPXTrack;
import com.example.trails.model.User;
import com.example.trails.repo.CommentRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class CommentService {

    private final CommentRepository commentRepository;

    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public List<Comment> findByTrackId(UUID trackId) {
        return commentRepository.findByTrack_IdOrderByCreatedAtDesc(trackId);
    }

    public Comment createComment(GPXTrack track, User user, String text) {
        Comment comment = new Comment();
        comment.setTrack(track);
        comment.setUser(user);
        comment.setText(text);
        return commentRepository.save(comment);
    }

    public void deleteComment(UUID id) {
        commentRepository.deleteById(id);
    }

    public Comment findById(UUID id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found: " + id));
    }
}
