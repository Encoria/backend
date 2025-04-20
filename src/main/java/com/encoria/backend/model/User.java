package com.encoria.backend.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator; // Import for GenericGenerator if needed
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator; // Import for newer Hibernate strategy

import java.time.LocalDate; // Use LocalDate for birthdate
import java.util.UUID;     // Use UUID type

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "uuid"),
        @UniqueConstraint(columnNames = "externalAuthId"),
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_seq")
    @SequenceGenerator(name = "users_seq", sequenceName = "users_seq", allocationSize = 50)
    @Column(nullable = false)
    private Long id;

    // --- UUID Changes ---
    //@GeneratedValue(strategy = GenerationType.UUID) // Strategy for JPA to know it's generated
    // OR for more modern Hibernate:
    @UuidGenerator(style = UuidGenerator.Style.RANDOM) // Specify UUIDv4 generation strategy
    @Column(nullable = false, updatable = false, unique = true) // UUID shouldn't be updated
    private UUID uuid; // Change type to java.util.UUID
    // --- End UUID Changes ---

    @Column(nullable = false)
    private String externalAuthId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String email;

    private String firstName;
    private String lastName;

    @Column(nullable = false)
    @Temporal(TemporalType.DATE) // Or remove if using LocalDate
    private LocalDate birthdate; // Changed to LocalDate

    private String pictureUrl;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private UserRole role;

    // --- Example: Other Default Values ---
    @Column(nullable = false)
    private boolean isEnabled = true;

    @Column(nullable = false)
    private boolean isNonLocked = true;

    // Timestamps managed by Hibernate
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private java.time.Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private java.time.Instant updatedAt;
    // --- End Example ---
}