package com.library.app.auth.controller;

import com.library.app.auth.model.LibraryUser;
import com.library.app.auth.model.LibraryUserRoles;
import com.library.app.auth.service.LibraryUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private LibraryUserService libraryUserService;

    // ---------------- GET ALL USERS (PAGINATED) ----------------
    @GetMapping
    public ResponseEntity<?> getAllUsers(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        logger.info("GET /users called with page={} & size={}", page, size);

        if (page < 0 || size <= 0) {
            logger.warn("Invalid pagination parameters: page={}, size={}", page, size);
            return ResponseEntity.badRequest().body("Invalid pagination parameters.");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("username").ascending());
        Page<LibraryUser> users = libraryUserService.getAllUsers(pageable);

        logger.info("Found {} users", users.getTotalElements());
        if (users.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return ResponseEntity.ok(users);
    }

    // ---------------- GET USER (BY ID OR USERNAME) ----------------
    @GetMapping("/user")
    public ResponseEntity<?> getUser(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String username
    ) {
        logger.info("GET /users/user called with id={} username={}", id, username);

        // Case 1: both null → invalid
        if (id == null && (username == null || username.trim().isEmpty())) {
            logger.warn("Missing parameters: both id and username are null/empty");
            return ResponseEntity.badRequest()
                    .body("Either 'id' or 'username' parameter must be provided.");
        }

        // Case 2: search by ID if provided
        if (id != null) {
            return libraryUserService.getUserById(id)
                    .<ResponseEntity<?>>map(user -> {
                        logger.info("✅ User found by ID {}: {}", id, user.getUsername());
                        return ResponseEntity.ok(user);
                    })
                    .orElseGet(() -> {
                        logger.warn("❌ User not found with ID: {}", id);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body("User not found with ID: " + id);
                    });
        }

        // Case 3: search by username if ID not provided
        return libraryUserService.getUserByUsername(username)
                .<ResponseEntity<?>>map(user -> {
                    logger.info("✅ User found by username '{}'", username);
                    return ResponseEntity.ok(user);
                })
                .orElseGet(() -> {
                    logger.warn("❌ User not found with username: {}", username);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("User not found with username: " + username);
                });
    }

    @GetMapping("/user/current")
    public ResponseEntity<?> getCurrentUser(
            Principal principal
    ){
        return  getUser(null, principal.getName());
    }

    // ---------------- UPDATE USER ----------------
    @PutMapping("/{id}/updateName")
    public ResponseEntity<?> updateUserName(@PathVariable Long id,
                                            @RequestParam String username,
                                            Principal principal) {
        logger.info("PUT /users/{}/updateName called by {} with new username={}", id, principal.getName(), username);
        Optional<LibraryUser> currentUser = libraryUserService.getUserByUsername(principal.getName());

        if (currentUser.isEmpty() || !currentUser.get().getId().equals(id)
                && !currentUser.get().getRoles().contains(LibraryUserRoles.ROLE_ADMIN)) {
            logger.warn("Unauthorized username update attempt by {}", principal.getName());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You are not allowed to update this password.");
        }
    try {
        return libraryUserService.updateUserName(id, username, principal.getName())
                .<ResponseEntity<?>>map(user -> {
                    logger.info("Updated user: {}", user);
                    return ResponseEntity.ok(user);
                })
                .orElseGet(() -> {
                    logger.warn("User not found with ID: {}", id);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
                });
    } catch (DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("Username already exists. Please choose a different username.");
    }
    }

    // ---------------- DISABLE USER ----------------
    @PatchMapping("/{id}/disable")
    public ResponseEntity<?> disableUser(@PathVariable Long id) {
        logger.info("PATCH /users/{}/disable called", id);
        return libraryUserService.disableUser(id)
                .<ResponseEntity<?>>map(user -> {
                    logger.info("User disabled: {}", user);
                    return ResponseEntity.ok(user);
                })
                .orElseGet(() -> {
                    logger.warn("User not found with ID: {}", id);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
                });
    }

    // ---------------- ENABLE USER ----------------
    @PatchMapping("/{id}/enable")
    public ResponseEntity<?> enableUser(@PathVariable Long id) {
        logger.info("PATCH /users/{}/enable called", id);
        return libraryUserService.enableUser(id)
                .<ResponseEntity<?>>map(user -> {
                    logger.info("User enabled: {}", user);
                    return ResponseEntity.ok(user);
                })
                .orElseGet(() -> {
                    logger.warn("User not found with ID: {}", id);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
                });
    }

    // ---------------- EXPIRE USER ----------------
    @PatchMapping("/{id}/expire")
    public ResponseEntity<?> expireUser(@PathVariable Long id) {
        logger.info("PATCH /users/{}/expire called", id);
        return libraryUserService.expireUser(id)
                .<ResponseEntity<?>>map(user -> {
                    logger.info("User expired: {}", user);
                    return ResponseEntity.ok(user);
                })
                .orElseGet(() -> {
                    logger.warn("User not found with ID: {}", id);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
                });
    }

    // ---------------- DELETE USER ----------------
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        logger.info("DELETE /users/{} called", id);
        try {
            Optional<LibraryUser> existing = libraryUserService.getUserById(id);
            if (existing.isEmpty()) {
                logger.warn("User not found with ID: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
            }

            libraryUserService.deleteUser(id);
            logger.info("User deleted: {}", existing.get());
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

        } catch (Exception e) {
            logger.error("Error deleting user {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting user: " + e.getMessage());
        }
    }

    // ---------------- UPDATE PASSWORD ----------------
    @PatchMapping("/{id}/password")
    public ResponseEntity<?> updatePassword(@PathVariable Long id,
                                            @RequestBody Map<String, String> body,
                                            Principal principal) {
        logger.info("PATCH /users/{}/password called by {}", id, principal.getName());
        logger.debug("Request body: {}", body);

        try {
            String newPassword = body.get("password");
            if (newPassword == null || newPassword.trim().isEmpty()) {
                logger.warn("Empty new password for user ID: {}", id);
                return ResponseEntity.badRequest().body("New password must not be empty.");
            }

            // Allow only ADMIN or the user themselves
            Optional<LibraryUser> currentUser = libraryUserService.getUserByUsername(principal.getName());

            if (currentUser.isEmpty() || !currentUser.get().getId().equals(id)
                    && !currentUser.get().getRoles().contains(LibraryUserRoles.ROLE_ADMIN)) {
                logger.warn("Unauthorized password update attempt by {}", principal.getName());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You are not allowed to update this password.");
            }

            Optional<LibraryUser> updatedUser = libraryUserService.updatePassword(id, newPassword);
            if (updatedUser.isPresent()) {
                logger.info("Password updated successfully for user ID: {}", id);
                return ResponseEntity.ok("Password updated successfully.");
            } else {
                logger.warn("User not found with ID: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found with ID: " + id);
            }
        } catch (Exception e) {
            logger.error("Error updating password for user {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating password: " + e.getMessage());
        }
    }

    // ---------------- SEARCH USERS BY NAME (PAGINATED) ----------------
    @GetMapping("/search")
    public ResponseEntity<?> searchUsersByName(@RequestParam String name,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        logger.info("GET /users/search called with name='{}', page={}, size={}", name, page, size);

        if (name == null || name.trim().isEmpty()) {
            logger.warn("Empty search name");
            return ResponseEntity.badRequest().body("Search name cannot be empty.");
        }
        if (page < 0 || size <= 0) {
            logger.warn("Invalid pagination parameters: page={}, size={}", page, size);
            return ResponseEntity.badRequest().body("Invalid pagination parameters.");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("username").ascending());
        Page<LibraryUser> users = libraryUserService.findByUsername(name, pageable);

        logger.info("Found {} users matching '{}'", users.getTotalElements(), name);
        if (users.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return ResponseEntity.ok(users);
    }
}
