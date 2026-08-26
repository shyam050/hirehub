package com.hirehub.user.entity;

import com.hirehub.common.enums.Role;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    @Column(unique = true)
    private String email;

    private String name;
    private String image;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "role_selected_at")
    private java.time.OffsetDateTime roleSelectedAt;

    @Column(name = "password_hash", length = 255)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String passwordHash;

    @Column(name = "is_anonymous")
    private Boolean isAnonymous = false;

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private java.time.OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.OffsetDateTime.now();
        updatedAt = java.time.OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.OffsetDateTime.now();
    }
}
