package com.encoria.backend.controller;

import com.encoria.backend.model.User;
import com.encoria.backend.model.UserProfileDto;
import com.encoria.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Endpoint for the user to submit their profile details
    // Use POST for creation, could also use PUT on /api/users/me
    @PostMapping("/profile")
    public ResponseEntity<?> setupUserProfile(@AuthenticationPrincipal Jwt jwt,
                                              @Valid @RequestBody UserProfileDto profileDto) {
        String externalAuthId = jwt.getSubject();
        String emailFromToken = jwt.getClaimAsString("email"); // Get email if available

        // Basic check: Ensure email in DTO matches token if provided
        // Add more robust validation in the service layer
        if (profileDto.getEmail() == null) {
            profileDto.setEmail(emailFromToken); // Use email from token if not submitted
        } else if (emailFromToken != null && !emailFromToken.equalsIgnoreCase(profileDto.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Email mismatch between token and request."));
        }

        try {
            User createdUser = userService.createUserProfile(externalAuthId, profileDto);
            // Return the created user profile or just success status
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser); // Or a UserProfileDto representation
        } catch (IllegalStateException e) { // Example: Catch username/email already exists
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // Log error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to create profile."));
        }
    }

    // Example: Endpoint to get the current user's data (will only work after profile setup)
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        String externalAuthId = jwt.getSubject();
        User user = userService.findByExternalAuthId(externalAuthId)
                .orElseThrow(() -> new RuntimeException("User not found, profile setup likely incomplete.")); // Should be caught by filter
        // Convert user to DTO and return
        return ResponseEntity.ok(user); // Replace with DTO mapping
    }

    @GetMapping("/you")
    public ResponseEntity<?> getUserEmail(@AuthenticationPrincipal Jwt jwt) {
        String externalAuthId = jwt.getSubject();
        User user = userService.findByExternalAuthId(externalAuthId)
                .orElseThrow(() -> new RuntimeException("User not found, profile setup likely incomplete.")); // Should be caught by filter
        // Convert user to DTO and return
        return ResponseEntity.ok(user); // Replace with DTO mapping
    }

    // Other user-related endpoints...
}
