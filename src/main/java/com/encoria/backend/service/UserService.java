package com.encoria.backend.service;

import com.encoria.backend.model.User;
import com.encoria.backend.model.UserProfileDto;
import com.encoria.backend.model.UserRole; // Import UserRole
import com.encoria.backend.repository.UserRepository;
import com.encoria.backend.repository.UserRoleRepository; // Import UserRoleRepository
import jakarta.persistence.EntityNotFoundException; // Import exception
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import Transactional

import java.util.Optional;

@Service
@RequiredArgsConstructor // Lombok handles constructor injection
public class UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository; // Inject UserRoleRepository

    public Optional<User> findByExternalAuthId(String externalAuthId) {
        return userRepository.findByExternalAuthId(externalAuthId);
    }

    @Transactional // Make this method transactional
    public User createUserProfile(String externalAuthId, @Valid UserProfileDto profileDto) {

        // --- Check if user already exists (optional but good practice) ---
        if (userRepository.existsByExternalAuthId(externalAuthId)) {
            throw new IllegalStateException("User with this external ID already exists.");
        }
        if (userRepository.existsByUsername(profileDto.getUsername())) {
            throw new IllegalStateException("Username '" + profileDto.getUsername() + "' is already taken.");
        }
        if (userRepository.existsByEmail(profileDto.getEmail())) {
            throw new IllegalStateException("Email '" + profileDto.getEmail() + "' is already registered.");
        }
        // --- End checks ---


        // --- Fetch the default role ---
        UserRole defaultRole = userRoleRepository.findByCode("USER") // Find by code 'USER'
                .orElseThrow(() -> new EntityNotFoundException("Default role 'USER' not found in database."));
        // --- End fetch ---

        User newUser = User.builder()
                .externalAuthId(externalAuthId)
                .username(profileDto.getUsername())
                .email(profileDto.getEmail())
                .firstName(profileDto.getFirstName())
                .lastName(profileDto.getLastName())
                .birthdate(profileDto.getBirthdate())
                .pictureUrl(profileDto.getPictureUrl())
                // --- ASSIGN THE ROLE ---
                .role(defaultRole)
                // isEnabled, isNonLocked, createdAt, updatedAt are handled by defaults/annotations
                .build();

        return userRepository.save(newUser);
    }

    // Add existsBy methods to UserRepository if they don't exist
    // boolean existsByExternalAuthId(String externalAuthId);
    // boolean existsByUsername(String username);
    // boolean existsByEmail(String email);
}
