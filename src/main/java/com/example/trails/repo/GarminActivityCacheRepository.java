package com.example.trails.repo;

import com.example.trails.model.GarminActivityCache;
import com.example.trails.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GarminActivityCacheRepository extends JpaRepository<GarminActivityCache, UUID> {
    Optional<GarminActivityCache> findByUser(User user);
}
