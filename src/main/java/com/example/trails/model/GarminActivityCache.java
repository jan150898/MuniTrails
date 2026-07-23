package com.example.trails.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

/**
 * Cache Garmin activities per user for faster subsequent logins.
 * Cached data is refreshable and expires after a configurable TTL.
 */
@Entity
@Table(name = "garmin_activity_cache")
public class GarminActivityCache {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT", nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(SqlTypes.JSON)
    private JsonNode activities; // Stored as JSON array

    @Column(nullable = false, updatable = false)
    private Instant cachedAt;

    @Column(nullable = false)
    private Instant expiresAt; // Auto-refresh after this time

    @PrePersist
    void onCreate() {
        cachedAt = Instant.now();
        expiresAt = cachedAt.plusSeconds(3600); // 1 hour TTL
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public JsonNode getActivities() {
        return activities;
    }

    public void setActivities(JsonNode activities) {
        this.activities = activities;
    }

    public Instant getCachedAt() {
        return cachedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
