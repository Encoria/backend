package com.encoria.backend.repository;

import com.encoria.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByExternalAuthId(String externalAuthId);

    boolean existsByExternalAuthId(String externalAuthId);

    // Method to check if username exists (needed for profile validation)
    boolean existsByUsername(String username);

    // Method to check if email exists (needed for profile validation)
    boolean existsByEmail(String email);

}
