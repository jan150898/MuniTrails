package com.example.trails.repo;

import com.example.trails.model.GPXTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface GPXTrackRepository extends JpaRepository<GPXTrack, UUID> {
}

