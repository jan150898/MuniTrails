package com.example.trails.service;

import com.example.trails.model.User;
import com.example.trails.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UserService
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("User Service Tests")
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Find user by username")
    void testFindByUsername() {
        User user = new User();
        user.setUsername("testuser");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole("ROLE_USER");
        userRepository.save(user);

        Optional<User> found = userService.findByUsername("testuser");

        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
    }

    @Test
    @DisplayName("Find by username returns empty when not found")
    void testFindByUsernameNotFound() {
        Optional<User> found = userService.findByUsername("nonexistent");

        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("Get user by username (throws exception)")
    void testGetUserByUsername() {
        User user = new User();
        user.setUsername("getme");
        user.setPasswordHash(passwordEncoder.encode("password"));
        user.setRole("ROLE_USER");
        userRepository.save(user);

        User found = userService.getUserByUsername("getme");

        assertNotNull(found);
        assertEquals("getme", found.getUsername());
    }

    @Test
    @DisplayName("Get user by username throws RuntimeException when not found")
    void testGetUserByUsernameNotFound() {
        assertThrows(RuntimeException.class, () -> {
            userService.getUserByUsername("nonexistent");
        });
    }

    @Test
    @DisplayName("Find user by ID")
    void testFindById() {
        User user = new User();
        user.setUsername("byid");
        user.setPasswordHash(passwordEncoder.encode("password"));
        user.setRole("ROLE_USER");
        user = userRepository.save(user);

        Optional<User> found = userService.findById(user.getId());

        assertTrue(found.isPresent());
        assertEquals("byid", found.get().getUsername());
    }

    @Test
    @DisplayName("Find by ID returns empty when not found")
    void testFindByIdNotFound() {
        Optional<User> found = userService.findById(UUID.randomUUID());

        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("User can be created with valid data")
    void testUserCreation() {
        User user = new User();
        user.setUsername("newuser");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole("ROLE_USER");
        user = userRepository.save(user);

        assertNotNull(user.getId());
        assertEquals("newuser", user.getUsername());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    @DisplayName("Multiple users can be created")
    void testMultipleUsers() {
        User user1 = new User();
        user1.setUsername("user1");
        user1.setPasswordHash("hash1");
        user1.setRole("ROLE_USER");
        userRepository.save(user1);

        User user2 = new User();
        user2.setUsername("user2");
        user2.setPasswordHash("hash2");
        user2.setRole("ROLE_USER");
        userRepository.save(user2);

        Optional<User> found1 = userService.findByUsername("user1");
        Optional<User> found2 = userService.findByUsername("user2");

        assertTrue(found1.isPresent());
        assertTrue(found2.isPresent());
        assertNotEquals(found1.get().getId(), found2.get().getId());
    }

    @Test
    @DisplayName("User username is unique")
    void testUsernameUniqueness() {
        User user1 = new User();
        user1.setUsername("duplicate");
        user1.setPasswordHash("hash1");
        user1.setRole("ROLE_USER");
        userRepository.save(user1);

        User user2 = new User();
        user2.setUsername("duplicate");
        user2.setPasswordHash("hash2");
        user2.setRole("ROLE_USER");

        assertThrows(Exception.class, () -> {
            userRepository.saveAndFlush(user2);
        });
    }

    @Test
    @DisplayName("User role can be set")
    void testUserRole() {
        User user = new User();
        user.setUsername("roletest");
        user.setPasswordHash("hash");
        user.setRole("ROLE_ADMIN");
        user = userRepository.save(user);

        assertEquals("ROLE_ADMIN", user.getRole());
    }

    @Test
    @DisplayName("User with bcrypt hashed password")
    void testBcryptPassword() {
        String plainPassword = "mySecurePassword123";
        User user = new User();
        user.setUsername("bcrypttest");
        user.setPasswordHash(passwordEncoder.encode(plainPassword));
        user.setRole("ROLE_USER");
        user = userRepository.save(user);

        assertTrue(passwordEncoder.matches(plainPassword, user.getPasswordHash()));
        assertFalse(passwordEncoder.matches("wrongPassword", user.getPasswordHash()));
    }

    @Test
    @DisplayName("User timestamps are automatically set")
    void testUserTimestamps() {
        User user = new User();
        user.setUsername("timestamps");
        user.setPasswordHash("hash");
        user.setRole("ROLE_USER");
        user = userRepository.save(user);

        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
        assertEquals(user.getCreatedAt(), user.getUpdatedAt());
    }
}
