package com.example.trails.repo;

import com.example.trails.model.GarminCredential;
import com.example.trails.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GarminCredentialRepository extends JpaRepository<GarminCredential, UUID> {
    Optional<GarminCredential> findByUser(User user);
    void deleteByUser(User user);
}
